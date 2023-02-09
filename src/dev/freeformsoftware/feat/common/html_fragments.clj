(ns dev.freeformsoftware.feat.common.html-fragments
  (:require
   [cheshire.core :as cheshire]
   [com.biffweb :as biff]
   [ring.middleware.anti-forgery :as csrf]
   [xtdb.api :as xt]))

(defn background
  "Includes CSRF for HTMX"
  [& body]
  [:.bg-orange-50.flex.flex-row.items-stretch.h-screen
   {:hx-headers (cheshire/generate-string
                 {:x-csrf-token csrf/*anti-forgery-token*})}
   body
   [:div.absolute#modal-container]])

(defn page-with-sidebar
  [sidebar
   body]
  (background
   [:.border-r-2.border-r-slate-900.w-64
    sidebar]
   body))

(defn user-description
  [{:user/keys [name]}]
  [:div [:span.font-light "Signed in as "] [:span.font-bold name] ". "
   (biff/form
    {:action "/auth/signout"
     :class "inline"}
    [:button.text-blue-500.hover:text-blue-800
     {:type "submit"}
     "Sign Out"])
   "."])

(defn sidebar
  [{:keys [session biff/db] :as req}
   center
   action]
  (let [user (xt/entity db (:uid session))]
    [:.flex.flex-col.items-stretch.h-screen.gap-4
     [:.mx-3.my-2.flex-grow-0
      (user-description user)]
     [:.flex-grow center]
     action
     [:.h-3]]))

(defn modal-container
  [contents]
  [:.flex.flex-row.items-center.justify-center.h-screen.w-screen.absolute.modal-container.min-h-fit.min-w-fit
   {:class ["bg-gray-50/75"]
    :_ "on closeModal remove me"}
   [:div {:class '[border-2 bg-orange-50 border-slate-900 min-w-fit min-h-fit "w-3/4" "max-w-lg" p-5]}
    contents]])

(defn right-aligned
  [contents]
  [:.self-end.flex contents])

(def input-classes '[border
                     border-gray-300
                     rounded
                     w-full
                     focus:border-teal-600
                     focus:ring-teal-600])

(def button-classes '[bg-teal-600
                      hover:bg-teal-800
                      text-white
                      py-1
                      text-center])

(def cancel-button-classes '[bg-red-500
                             hover:bg-red-300
                             text-white
                             py-1
                             text-center])

(defn form-modal
  [title fields button-bar]
  [:.w-full.h-full.flex.flex-col
   [:h3.self-start.text-2xl.text-extrabold
    title]
   [:.h-4]
   [:.self-stretch.flex-grow
    fields]
   [:.h-4]
   button-bar])

(defn tooltip
  [trigger contents]
  [:div.flex.items-center.justify-center.popup
   [:.relative.flex.flex-col.items-center.cursor-pointer
    [:.relative.w-full.h-0.popup-container.hidden
     {:style {:opacity 0}}
     [:div {:class "absolute bottom-0 inline-block w-64 px-4 py-3 mb-2 left-1/2 -ml-32 text-white bg-gray-600"}
      [:span.inline-block.text-sm.leading-tight
       contents]]]
    [:div {:_ "on mouseover remove .hidden from the previous .popup-container then transition the previous .popup-container opacity to 100 over 125ms
                 on mouseout transition the previous .popup-container opacity to 0 over 125ms then add .hidden to the previous .popup-container"}
     trigger]]])

(defn form-input
  [{:keys [type label id placeholder name] :as input-props} & input-body]
  [:<>
   [:label {:for id} label]
   [:br]
   [(case type
      "textarea" :textarea
      "select" :select
      :input)
    (cond-> input-props
      (not placeholder) (assoc :placeholder label)
      (not name) (assoc :name id))
    input-body]])
