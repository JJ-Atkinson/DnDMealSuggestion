(ns dev.freeformsoftware.feat.suggestion
  (:require
   [clojure.string :as str]
   [com.biffweb :as biff]
   [dev.freeformsoftware.feat.common.html-fragments :as frag]
   [dev.freeformsoftware.feat.image-search :as feat.image-search]
   [com.wsscode.transito :as transito]
   [jsonista.core :as json]
   [dev.freeformsoftware.middleware :as mid]
   [xtdb.api :as xt])
  (:import (java.util UUID)))

(def td :td.border-2.border-slate-900.p-2)
(def th :th.border-2.border-slate-900.p-2)

(def suggestion-table-head
  [:tr
   [th "Tried?"]
   [th "Name"]
   [th "Notes"]
   [th "Author"]
   [th "Votes"]
   [th "Food Type"]
   [th "Image"]
   [th "Actions"]])

(def suggestion-pull
  ['*
   {:suggestion/author [:user/name]}
   {:suggestion/voted-for-by [:user/name :xt/id]}])

(defn tr-list-item-fragment
  [user {:suggestion/keys [tried-before? name notes author
                           voted-for-by added-at food-type image-url] :as suggestion}]
  (let [id (:xt/id suggestion)
        user-is-self? #(= (:xt/id user) (:xt/id %))
        voted-for-by-ids (map :xt/id voted-for-by)
        self-voted-for-suggestion? (some user-is-self? voted-for-by)
        new-voted-for-by (if self-voted-for-suggestion?
                           (disj (set voted-for-by-ids) (:xt/id user))
                           (conj (set voted-for-by-ids) (:xt/id user)))
        hx-props (fn [new-props] {:hx-post (str "/app/suggestion/merge/" (.toString id))
                                  :hx-target "closest tr"
                                  :hx-swap "outerHTML"
                                  :hx-vals (json/write-value-as-string {:clj (transito/write-str new-props)})})]
    [:tr
     [td {:class '[text-center]}
      [:input (merge (hx-props {:suggestion/tried-before? (not tried-before?)})
                     {:type "checkbox"
                      :checked tried-before?})]]
     [td [:span name]]
     [td [:span notes]]
     [td [:span (:user/name author)]]
     [td (frag/tooltip
          [:span.font-bold.border-2.p-1.hover:border-teal-800.hover:bg-teal-200
           (merge (hx-props {:suggestion/voted-for-by new-voted-for-by})
                  {:class (if self-voted-for-suggestion?
                            '[border-teal-600 bg-teal-100]
                            '[border-slate-900])})
           (str (count voted-for-by) " 👍")]
          (or (seq (map (fn [{:keys [user/name]}]
                          [:<> [:span name] [:br]])
                        voted-for-by))
              [:span "No votes yet!"]))]
     [td [:span (get {:food-type/meal "Meal"
                      :food-type/snack "Snack"}
                     food-type)]]
     [td [:img.w-32 {:src image-url :alt "Can't load"}]]
     [td {:class '[text-right]}
      [:button.cursor-pointer.text-white.bg-slate-500.hover:bg-red-300.hover:text-black.py-1.px-3.text-center
       {:hx-delete (str "/app/suggestion/delete/" (.toString id))}
       "X"]]]))

(defn items-for-list
  [{:keys [biff/db]} list-id]
  (biff/lookup-all db suggestion-pull :suggestion/list list-id))

(defn new-suggestion-button
  [{{:keys [list-name]} :path-params}]
  (frag/right-aligned
   [:button.cursor-pointer
    {:hx-get (str "/app/new-suggestion/" list-name)
     :hx-target "#modal-container"
     :class (conj frag/button-classes "px-10")}
    "+ Add Suggestion"]))

(defn suggestion-table
  [{{:keys [list-name]} :path-params :keys [biff/db session] :as req}]
  (let [{:keys [xt/id]} (biff/lookup db :list/name list-name)
        items (items-for-list req id)
        user (xt/entity db (:uid session))]

    [:<>
     [:.h-4]
     [:table.border-collapse
      suggestion-table-head
      (map (partial tr-list-item-fragment user) items)]
     [:.h-4]
     (new-suggestion-button req)]))

(def food-type-options
  [{:clj-value :food-type/meal
    :label "Meal"}
   {:clj-value :food-type/snack
    :label "Snack"}])

(defn new-suggestion-fragment
  [{{:keys [list-name]} :path-params}]
  (frag/modal-container
   (biff/form
    {:id "make-suggestion-form"
     :hx-post (str "/app/new-suggestion/" list-name)
     :class "w-full h-full"}
    (frag/form-modal
     "Add a new suggestion"
     [:.grid.gap-2.grid-cols-2
      [:.col-span-2
       (frag/form-input {:id "name"
                         :label "Name"
                         :type "text"
                         :class frag/input-classes})]
      [:.col-span-1
       (feat.image-search/form-input-frag nil)]
      [:.col-span-1
       (frag/form-input {:id "notes"
                         :label "Notes"
                         :type "textarea"
                         :class frag/input-classes})]
      [:.col-span-1
       (frag/form-input {:id "tried-before"
                         :label "Tried Before?"
                         :type "checkbox"
                         :checked false})]
      [:.col-span-1
       (frag/form-input
        {:id "food-type"
         :label "Food Type"
         :type "select"
         :class frag/input-classes}
        (map (fn [o] [:option {:value (pr-str (:clj-value o))} (:label o)]) food-type-options))]]
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

(defn post-new-suggestion
  [{{:keys [tried? name notes food-type image-url]} :params
    {:keys [list-name]} :path-params
    :keys [session biff/db params] :as req}]
  (let [user (xt/entity db (:uid session))
        list (biff/lookup db :list/name list-name)]
    (assert (not (str/blank? name)))
    (biff/submit-tx req
                    [{:db/doc-type :suggestion
                      :db/op :upsert
                      :suggestion/tried-before? (= tried? "on")
                      :suggestion/name name
                      :suggestion/notes notes
                      :suggestion/author (:xt/id user)
                      :suggestion/voted-for-by #{(:xt/id user)}
                      :suggestion/added-at :db/now
                      :suggestion/food-type (read-string food-type)
                      :suggestion/list (:xt/id list)
                      :suggestion/image-url image-url}])
    {:status 200
     :headers {"HX-Refresh" "true"}}))

(defn post-merge-suggestion
  [{:keys [params biff/db session biff.xtdb/node] {:keys [suggestion-id]} :path-params :as req}]
  (let [id (UUID/fromString suggestion-id)
        user (xt/entity db (:uid session))]
    (biff/submit-tx req
                    [(merge {:db/doc-type :suggestion
                             :db/op :merge
                             :xt/id id}
                            (transito/read-str (:clj params)))])
    (tr-list-item-fragment user (xt/pull (xt/db node) suggestion-pull id))))

(defn delete-suggestion
  [{{:keys [suggestion-id]} :path-params :as req}]
  (let [id (UUID/fromString suggestion-id)]
    (biff/submit-tx req
                    [{:db/doc-type :suggestion
                      :db/op :delete
                      :xt/id id}])
    {:status 200
     :headers {"HX-Refresh" "true"}}))

(def features
  {:routes ["/app" {:middleware [mid/wrap-signed-in]}
            ["/new-suggestion/:list-name" {:post post-new-suggestion
                                           :get new-suggestion-fragment}]
            ["/suggestion/merge/:suggestion-id" {:post post-merge-suggestion}]
            ["/suggestion/delete/:suggestion-id" {:delete delete-suggestion}]]})
