(ns dev.freeformsoftware.feat.app
  (:require
   [com.biffweb :as biff :refer [q]]
   [dev.freeformsoftware.middleware :as mid]
   [dev.freeformsoftware.ui :as ui]
   [xtdb.api :as xt]
   [dev.freeformsoftware.feat.list :as feat.list]
   [dev.freeformsoftware.feat.common.html-fragments :as frag]))

(defn set-foo [{:keys [session params] :as req}]
              (biff/submit-tx req
                              [{:db/op :update
                                :db/doc-type :user
                                :xt/id (:uid session)
                                :user/foo (:foo params)}])
              {:status 303
               :headers {"location" "/app"}})

(defn bar-form [{:keys [value]}]
               (biff/form
                {:hx-post "/app/set-bar"
                 :hx-swap "outerHTML"}
                [:label.block {:for "bar"} "Bar: "
                 [:span.font-mono (pr-str value)]]
                [:.h-1]
                [:.flex
                 [:input.w-full#bar {:type "text" :name "bar" :value value}]
                 [:.w-3]
                 [:button.btn {:type "submit"} "Update"]]
                [:.h-1]
                [:.text-sm.text-gray-600
                 "This demonstrates updating a value with HTMX."]))

(defn set-bar [{:keys [session params] :as req}]
              (biff/submit-tx req
                              [{:db/op :update
                                :db/doc-type :user
                                :xt/id (:uid session)
                                :user/bar (:bar params)}])
              (biff/render (bar-form {:value (:bar params)})))

(defn message [{:msg/keys [text sent-at]}]
              [:.mt-3 {:_ "init send newMessage to #message-header"}
               [:.text-gray-600 (biff/format-date sent-at "dd MMM yyyy HH:mm:ss")]
               [:div text]])

(defn chat [{:keys [biff/db]}]
           (let [messages (q db
                             '{:find (pull msg [*])
                               :in [t0]
                               :where [[msg :msg/sent-at t]
                                       [(<= t0 t)]]}
                             (biff/add-seconds (java.util.Date.) (* -60 10)))]
             [:div {:hx-ws "connect:/app/chat"}
              [:form.mb0 {:hx-ws "send"
                          :_ "on submit set value of #message to ''"}
               [:label.block {:for "message"} "Write a message"]
               [:.h-1]
               [:textarea.w-full#message {:name "text"}]
               [:.h-1]
               [:.text-sm.text-gray-600
                "Sign in with an incognito window to have a conversation with yourself."]
               [:.h-2]
               [:div [:button.btn {:type "submit"} "Send message"]]]
              [:.h-6]
              [:div#message-header
               {:_ "on newMessage put 'Messages sent in the past 10 minutes:' into me"}
               (if (empty? messages)
                 "No messages yet."
                 "Messages sent in the past 10 minutes:")]
              [:div#messages
               (map message (sort-by :msg/sent-at #(compare %2 %1) messages))]]))

(defn app [{:keys [session biff/db] :as req}]
          (let [{:user/keys [name foo bar]} (xt/entity db (:uid session))]
            (ui/page
             {}
             nil
             [:div "Signed in as " name ". "
              (biff/form
               {:action "/auth/signout"
                :class "inline"}
               [:button.text-blue-500.hover:text-blue-800 {:type "submit"}
                "Sign out"])
              "."]
             [:.h-6]
             (biff/form
              {:action "/app/set-foo"}
              [:label.block {:for "foo"} "Foo: "
               [:span.font-mono (pr-str foo)]]
              [:.h-1]
              [:.flex
               [:input.w-full#foo {:type "text" :name "foo" :value foo}]
               [:.w-3]
               [:button.btn {:type "submit"} "Update"]]
              [:.h-1]
              [:.text-sm.text-gray-600
               "This demonstrates updating a value with a plain old form."])
             [:.h-6]
             (bar-form {:value bar})
             [:.h-6]
             (chat req))))









(defn app-new
  [{:keys [session biff/db] :as req}]
  (ui/base
   {}
   (frag/page-with-sidebar
    (frag/sidebar
     req
     (feat.list/lists-markup req) 
     (feat.list/new-list-button req))
    nil)))

(def features
  {:routes ["/app" {:middleware [mid/wrap-signed-in]}
            ["" {:get app-new}]]})
