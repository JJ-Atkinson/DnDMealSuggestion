(ns dev.freeformsoftware.feat.list
  (:require
   [clojure.pprint :as pprint]
   [clojure.string :as str]
   [com.biffweb :as biff]
   [dev.freeformsoftware.feat.common.html-fragments :as frag]
   [dev.freeformsoftware.middleware :as mid]
   [dev.freeformsoftware.ui :as ui]
   [dev.freeformsoftware.feat.suggestion :as feat.suggestion]
   [xtdb.api :as xt]))

(defn existing-lists
  [req]
  (biff/q (:biff/db req)
          '{:find (pull list [:xt/id :list/name])
            :where [[list :list/name]]}))

(defn lists-markup
  [{{:keys [list-name]} :path-params :as req}]
  (let [existing-lists (existing-lists req)]
    (if-not (seq existing-lists)
      [:.bg-orange-200.p-3 "No lists have been created yet. Make a new one?"]
      (map (fn [{:keys [xt/id list/name]}]
             [:.flex.flex-row
              [:a.block.cursor-pointer.py-2.flex-grow
               {:class (if (= list-name name)
                         '[bg-slate-500 text-white pl-6]
                         '[bg-slate-700 hover:bg-slate-200 text-white hover:text-black pl-4])
                :href (str "/app/list/" name)}
               [:span name]]
              [:a.block.cursor-pointer.w-10.text-bold.text-white.flex.justify-center.items-center.hover:bg-red-500.hover:text-black
               {:class (if (= list-name name) '[bg-slate-500] '[bg-slate-700])
                :hx-delete (str "/app/list/" name)
                :hx-swap "delete"
                :hx-target "closest div"}
               [:span "X"]]])
           existing-lists))))

(defn new-list-button
  [req]
  [:a.mx-2.cursor-pointer
   {:hx-get "/app/new-list"
    :hx-target "#modal-container"
    :class frag/button-classes}
   "+ New List"])

(defn new-list-fragment
  [req]
  (frag/modal-container
   (biff/form
    {:id "make-list-form"
     :hx-target "closest .modal-container"
     :hx-post "/app/new-list"
     :hx-swap "delete"
     :class "w-full h-full"}
    (frag/form-modal
     "Add a new list"
     [:.flex.flex-row.items-stretch.gap-1
      [:.w-full
       (frag/form-input {:id "list-name"
                         :type "text"
                         :label "List Name"
                         :class frag/input-classes})]]
     (frag/right-aligned
      [:<>
       [:button.cursor-pointer.text-white.bg-red-500.hover:bg-red-300.hover:text-black.py-1.text-center
        {:class (conj frag/cancel-button-classes "px-10")
         :_ "on click trigger closeModal"}
        "Cancel"]
       [:.w-4]
       [:input.cursor-pointer
        {:class (conj frag/button-classes "px-10")
         :type "submit"
         :value "+ Add suggestion"}]])))))

(defn first-existing-list-name
  [{:keys [biff/db]}]
  (first (biff/q db
                 '{:find ?name
                   :where [[_ :list/name ?name]]})))

(defn list-body
  [req {:keys [list/name xt/id] :as list}]
  [:.flex.items-center.justify-center.w-full
   [:.flex.flex-col.min-h-screen
    {:class ["min-w-fit w-8/12 max-w-3xl pt-8"]}
    [:h-3.text-3xl name]
    (feat.suggestion/suggestion-table req)]])

(defn existing-list-page
  [{{:keys [list-name]} :path-params :keys [biff/db] :as req}]
  (if-let [list (biff/lookup db :list/name list-name)]
    (ui/base
     {}
     (frag/page-with-sidebar
      (frag/sidebar
       req
       (lists-markup req)
       (new-list-button req))
      (list-body req list)))
    {:status 302
     :headers {"location" (str "/app/list/" (first-existing-list-name req))}}))

(defn post-new-list
  [{{:keys [list-name]} :params :as req}]
  (let [list-name (str/trim list-name)]
    (biff/submit-tx req
                    [{:db/doc-type :list
                      :db/op :upsert
                      :list/name [:db/unique list-name]}])
    {:status 200
     :headers {"HX-Redirect" (str "/app/list/" list-name)}}))

(defn delete-list
  [{{:keys [list-name]} :path-params :keys [biff/db] :as req}]
  (biff/submit-tx req
                  [(assoc (biff/lookup db '[:xt/id] :list/name list-name)
                          :db/op :delete)])
  {:status 200})

(def features
  {:routes ["/app" {:middleware [mid/wrap-signed-in]}
            ["/new-list" {:get new-list-fragment
                          :post post-new-list}]
            ["/list/:list-name" {:get existing-list-page
                                 :delete delete-list}]]})
