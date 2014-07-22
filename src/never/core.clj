(ns never.core
  (:use [clojure.tools.trace]
        [clojure.java.shell :only [sh]]) 
  (:require [clojure.data.json :as json]
            [net.cgrand.enlive-html :as html]
            [org.httpkit.client :as http]
            [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.jobs :refer [defjob]]
            [clojurewerkz.quartzite.jobs :as j]
            [clojurewerkz.quartzite.triggers :as t]
            [clojurewerkz.quartzite.schedule.simple :refer [schedule with-repeat-count with-interval-in-milliseconds]]))

(def url-list ["http://piao.mtime.com/service/online.p?t=201472221373869844&Ajax_CallBack=true&Ajax_CallBackType=Mtime.Piao.Pages.OnlineService&Ajax_CallBackMethod=GetHallSeatingByShowtimeId&Ajax_RequestUrl=http%3A%2F%2Fpiao.mtime.com%2Fonlineticket%2F2445_99134327%2Fseat%2F&Ajax_CallBackArgument0=99134327" "http://piao.mtime.com/service/online.p?t=201472222292463725&Ajax_CallBack=true&Ajax_CallBackType=Mtime.Piao.Pages.OnlineService&Ajax_CallBackMethod=GetHallSeatingByShowtimeId&Ajax_RequestUrl=http%3A%2F%2Fpiao.mtime.com%2Fonlineticket%2F2445_99197735%2Fseat%2F&Ajax_CallBackArgument0=99197735"])

(def cookies ["_userCode_=2014722201871214; _userIdentity_=2014722201875919; DefaultCity-CookieKey=1332; DefaultDistrict-CookieKey=0; searchHistoryCookie=%u6606%u5C71; Hm_lvt_6dd1e3b818c756974fb222f0eae5512e=1406031732,1406031734,1406031818,1406032885; Hm_lpvt_6dd1e3b818c756974fb222f0eae5512e=1406036258; __utma=8162739.1439622205.1406032494.1406032494.1406032494.1; __utmb=8162739.30.10.1406032494; __utmc=8162739; __utmz=8162739.1406032494.1.1.utmcsr=theater.mtime.com|utmccn=(referral)|utmcmd=referral|utmcct=/China_Jiangsu_Province_Kunshan_Kaifaqu/2445/"])

(def options {:timeout 200             ; ms
              :headers {"Host" "piao.mtime.com"
                        "Cookie" (first cookies)}})

(defn parse-url
  [url]
  (let [resp (http/get url options)
        body (:body @resp)
        ;body (html/html-snippet body)
        json (json/read-str (slurp body))
        cnt (-> json (get "value") (get "enableSeatCount"))]
    (println cnt)
    (if (< cnt 20)
      (do
        (sh "vlc" "./resources/normal.mp3")
        true)
      false)))

(def tk (t/key "triggers.1"))
(def jk (j/key "jobs.noop.1"))

(defrecord NoOpJob []
  org.quartz.Job
  (execute [this ctx]
    ;; intentional no-op
    (doseq [url url-list]
      (let [res (parse-url url)]
        (if res
          (qs/delete-job jk))))))

(defn -main
  []
  (qs/initialize)
  (qs/start)
  ;(parse-url (second url-list))
  (let [job (j/build
             (j/of-type NoOpJob)
             (j/with-identity jk))
        trigger (t/build
                 (t/with-identity tk)
                 (t/start-now)
                 (t/with-schedule (schedule
                                   (with-repeat-count 10000000)
                                   (with-interval-in-milliseconds (* 1000 60)))))]
    (qs/schedule job trigger)))
