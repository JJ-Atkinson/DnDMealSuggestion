(ns dev.freeformsoftware.schema
  (:require [malli.core :as malc]
            [malli.registry :as malr]))

(def schema
  {:user/id :uuid
   :user/discord-userid :int
   :user/name :string
   :user/joined-at inst?
   :user [:map {:closed true}
          [:xt/id :user/id]
          :user/discord-userid
          :user/name
          :user/joined-at]

   :list/id :uuid
   :list/name :string
   :list [:map {:closed true}
          [:xt/id :list/id]
          :list/name]

   :suggestion/id :uuid
   :suggestion/tried-before? :boolean
   :suggestion/name :string
   :suggestion/notes :string
   :suggestion/author :user/id
   :suggestion/voted-for-by [:set :user/id]
   :suggestion/added-at inst?
   :suggestion/food-type [:enum :food-type/meal :food-type/snack]
   :suggestion/image-url :string
   :suggestion/list :list/id
   :suggestion [:map {:closed true}
                [:xt/id :suggestion/id]
                :suggestion/tried-before?
                :suggestion/name
                :suggestion/notes
                :suggestion/author
                :suggestion/voted-for-by
                :suggestion/added-at
                :suggestion/food-type
                :suggestion/list
                :suggestion/image-url]})

(def malli-opts {:registry (malr/composite-registry malc/default-registry schema)})
