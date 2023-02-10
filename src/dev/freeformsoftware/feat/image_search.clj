(ns dev.freeformsoftware.feat.image-search
  (:require
   [clojure.string :as str]
   [com.wsscode.transito :as transito]
   [dev.freeformsoftware.feat.common.html-fragments :as frag]
   [dev.freeformsoftware.feat.common.unsplash :as unsplash]
   [dev.freeformsoftware.middleware :as mid]
   [jsonista.core :as json]))

(defn image-results-frag
  "image-query? is a string that is used to fill the image query input box when setting the result value."
  [results showing? image-query?]
  [:.relative#image-results-box
   [:.relative.w-0.h-0
    {:style {:max-width "0"}}
    [:.grid.grid-cols-2.p-4.bg-gray-600.overflow-auto.gap-4.rounded
     {:class [(when-not showing? "hidden")
              "h-96" "-ml-4 mt-2"]
      :style {:width "25rem"
              :filter "drop-shadow(0 25px 25px rgb(0 0 0 / 0.15))"}}
     (map (fn [{:keys [image-url profile-url profile-name]}]
            [:.flex.flex-col.cursor-pointer
             [:img.w-40 {:src image-url
                         :hx-trigger "click"
                         :hx-post "/app/image-search/post-value"
                         :hx-vals (json/write-value-as-string {:clj (transito/write-str {:image-query image-query?
                                                                                         :image-url image-url})})
                         :hx-target "closest .image-input-form"
                         :hx-swap "outerHTML"}]
             [:a.text-white {:href profile-url
                             :target "_blank"} profile-name]])
          results)]]])

(defn form-input-frag
  [{:keys [image-query image-url]}]
  [:div.image-input-form
   (frag/form-input (cond-> {:id "image-query"
                             :label "Image"
                             :type "text"
                             :class frag/input-classes
                             :hx-post "/app/image-search/get-results"
                             :hx-trigger "keyup changed delay:500ms, image-query"
                             :hx-target "#image-results-box"
                             :hx-swap "outerHTML"
                             :value image-query}
                      image-url
                      (assoc :_ "on keyup add .hidden to #current-result-image")))
   (frag/form-input {:id "image-url"
                     :type "text"
                     :value image-url
                     :class ["hidden"]})
   (when image-url
     [:img.w-40.mt-2#current-result-image {:src image-url}])
   (image-results-frag [] false image-query)])

(defn get-results
  "A post request that updates the result box"
  [{{:keys [image-query]} :params :keys [biff/secret params] :as req}]
  (println "get-results" (pr-str image-query))
  (let [query (str/trim image-query)]
    (if (str/blank? query)
      (image-results-frag [] false query)
      (let [results (unsplash/get-results (secret ::unsplash/token) image-query)]
        (image-results-frag results true query)))))

(defn post-value
  "A post request that hides the result box and sets the hidden form input `image-url`"
  [{{:keys [clj]} :params :as req}]
  (let [params (transito/read-str clj)]
    (form-input-frag params)))

(def features
  {:routes ["/app/image-search" {:middleware [mid/wrap-signed-in]}
            ["/get-results" {:post get-results}]
            ["/post-value" {:post post-value}]]})
