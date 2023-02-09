(ns dev.freeformsoftware.feat.home
  (:require [com.biffweb :as biff]
            [dev.freeformsoftware.middleware :as mid]
            [dev.freeformsoftware.ui :as ui]
            [dev.freeformsoftware.util :as util]
            [xtdb.api :as xt]))

(defn recaptcha-disclosure [{:keys [link-class]}]
                           [:span "This site is protected by reCAPTCHA and the Google "
                            [:a {:href "https://policies.google.com/privacy"
                                 :target "_blank"
                                 :class link-class}
                             "Privacy Policy"] " and "
                            [:a {:href "https://policies.google.com/terms"
                                 :target "_blank"
                                 :class link-class}
                             "Terms of Service"] " apply."])

(defn signin-form
  [{:keys [recaptcha/site-key] :as sys}]
  (biff/form
   {:id "signin-form"
    :action "/auth/send"
    :class "sm:max-w-xs w-full"}
   [:input#email
    {:name "discord-username"
     :type "text"
     :placeholder "Enter your discord username"
     :class '[border
              border-gray-300
              rounded
              w-full
              focus:border-teal-600
              focus:ring-teal-600]}]
   [:.h-3]
   [:button
    (merge
     {:data-sitekey site-key
      :data-callback "onSubscribe"
      :data-action "subscribe"}
     {:type "submit"
      :class '[bg-teal-600
               hover:bg-teal-800
               text-white
               py-2
               px-4
               rounded
               w-full
               g-recaptcha]})
    "Sign in"]))

(def recaptcha-scripts
  [[:script {:src "https://www.google.com/recaptcha/api.js"
             :async "async"
             :defer "defer"}]
   [:script (biff/unsafe
             (str "function onSubscribe(token) { document.getElementById('signin-form').submit(); }"))]])

(defn home
  [sys]
  (ui/base
   {:base/head recaptcha-scripts}
   [:.bg-orange-50.flex.flex-col.flex-grow.items-center.p-3
    [:.h-12.grow]
    [:img.w-40 {:src "/img/pot-of-stew.png"}]
    [:.h-6]
    [:.text-2xl.sm:text-3xl.font-semibold.sm:text-center.w-full
     "DnD Meal Suggestion Tracker"]
    [:.h-2]
    [:.h-6]
    (signin-form sys)
    [:.h-12 {:class "grow-[2]"}]
    [:.text-sm (recaptcha-disclosure {:link-class "link"})]
    [:.h-6]]))



(def features
  {:routes ["" {:middleware [mid/wrap-redirect-signed-in]}
            ["/" {:get home}]]})

