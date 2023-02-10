(ns dev.freeformsoftware.feat.common.unsplash
  (:require [clj-http.client :as http]))

(def ^:dynamic *unsplash-token* nil)

(def api-url
  "https://api.unsplash.com")

(defn unsplash-get
  [url-ext options]
  (http/get (str api-url url-ext)
            (assoc-in options [:headers "Authorization"] (str "Client-ID " *unsplash-token*))))

(defn get-results
  [token query-str]
  (binding [*unsplash-token* token]
    (map (fn [{:keys [urls links user] :as res}]
           {:image-url (:small urls)
            :profile-url (:html links)
            :profile-name (:first_name user)})
         (get-in (unsplash-get "/search/photos"
                               {:query-params {:query query-str
                                               :orientation "squarish"
                                               :per_page 20}
                                :as :json})
                 [:body :results]))))
