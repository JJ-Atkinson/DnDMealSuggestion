(ns dev.freeformsoftware.feat.auth.discord-send
  (:require
   [clj-http.client :as http]
   [clojure.string :as str]))

(def ^:dynamic *discord-token* nil)

(def misnomer-server-id 922907740874088458)

(def api-url
  "https://discord.com/api/v10")

(defn discord-get
  [url-ext options]
  (http/get (str api-url url-ext)
            (assoc-in options [:headers "Authorization"] (str "Bot " *discord-token*))))

(defn discord-post
  [url-ext options]
  (http/post (str api-url url-ext)
             (assoc-in options [:headers "Authorization"] (str "Bot " *discord-token*))))

(def discord-guild-members
  (memoize
   (fn [guild-id]
     (:body
      (discord-get
       (str "/guilds/" guild-id "/members")
       {:query-params {"limit" 20}
        :as :json})))))

(defn locate-user
  [guild-id partial-username-or-nickname]
  (let [[without-discriminator] (str/split partial-username-or-nickname #"#")
        match-against (str/lower-case without-discriminator)]
    (->> (discord-guild-members guild-id)
         (mapcat (fn [{:keys [user nick]}] [[nick user]
                                            [(:username user) user]]))
         (some (fn [[name user]] (and name (str/starts-with? (str/lower-case name) match-against) user))))))

(defn create-dm!
  [user-id]
  (:body (discord-post "/users/@me/channels"
                       {:content-type :json
                        :form-params {:recipient_id user-id}
                        :as :json})))

(defn create-message!
  [channel-id content-str]
  (let [nonce (rand-int Integer/MAX_VALUE)
        {:keys [body]} (discord-post (str "/channels/" channel-id "/messages")
                                     {:content-type :json 
                                      :form-params {:nonce nonce
                                                    :content content-str}
                                      :as :json})]
    (when (= (:nonce body) nonce)
      :sent)))

(defn send-message-to!
  [discord-token user-id message]
  (binding [*discord-token* discord-token]
    (create-message! (:id (create-dm! user-id)) message)))
