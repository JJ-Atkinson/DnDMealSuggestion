(ns dev.freeformsoftware.feat.auth
  (:require
   [com.biffweb :as biff]
   [dev.freeformsoftware.ui :as ui]
   [dev.freeformsoftware.feat.auth.discord-send :as auth.discord-send]
   [clj-http.client :as http]
   [xtdb.api :as xt]))

(defn human?
  [{:keys [biff/secret params]}]
  (let [{:keys [success score] :as body}
        (:body
         (http/post "https://www.google.com/recaptcha/api/siteverify"
                    {:form-params {:secret (secret :recaptcha/secret-key)
                                   :response (:g-recaptcha-response params)}
                     :as :json}))]
    (and success (or (nil? score) (<= 0.5 score)))))

(defn discord-user-for
  [req]
  (binding [auth.discord-send/*discord-token* ((:biff/secret req) ::auth.discord-send/token)]
    (try
      (auth.discord-send/locate-user
       auth.discord-send/misnomer-server-id
       (get-in req [:params :discord-username]))
      (catch Exception e (println e)))))

(defn send-link!
  [req discord-userid url]
  (println "dsuid" discord-userid ((:biff/secret req) ::auth.discord-send/token))
  (and (human? req)
       (auth.discord-send/send-message-to!
        ((:biff/secret req) ::auth.discord-send/token)
        discord-userid
        (str "Sign in to DND meal tracker at " url))))

(defn send-token
  [{:keys [biff/base-url
           biff/secret
           anti-forgery-token
           params]
    :as req}]
  (let [{:keys [id username] :as dsu} (discord-user-for req)
        user-id (some-> id (Long/parseLong))
        token (biff/jwt-encrypt
               {:intent "signin"
                :discord-userid user-id
                :discord-username username
                :state (biff/sha256 anti-forgery-token)
                :exp-in (* 60 60)}
               (secret :biff/jwt-secret))
        url (str base-url "/auth/verify/" token)]
    (println "DSU" dsu)
    {:status 303
     :headers {"location" (if (send-link! req user-id url)
                            "/auth/sent/"
                            "/auth/fail/")}}))

(defn verify-token
  [{:keys [biff.xtdb/node
           biff/secret
           path-params
           session
           anti-forgery-token] :as req}]
  (let [{:keys [intent discord-userid state discord-username]} (biff/jwt-decrypt (:token path-params)
                                                                (secret :biff/jwt-secret))
        success (and (= intent "signin")
                     (= state (biff/sha256 anti-forgery-token)))
        get-user-id #(biff/lookup-id (xt/db node) :user/discord-userid discord-userid)
        existing-user-id (when success (get-user-id))]
    (when (and success (not existing-user-id))
      (biff/submit-tx req
                      [{:db/doc-type :user
                        :db.op/upsert {:user/discord-userid discord-userid
                                       :user/name discord-username}
                        :user/joined-at :db/now}]))
    (if-not success
      {:status 303
       :headers {"location" "/auth/fail/"}}
      {:status 303
       :headers {"location" "/app"}
       :session (assoc session :uid (or existing-user-id (get-user-id)))})))

(defn signout [{:keys [session]}]
              {:status 303
               :headers {"location" "/"}
               :session (dissoc session :uid)})

(def signin-printed
  (ui/page
   {}
   [:div
    "The sign-in link was printed to the console. If you add API "
    "keys for Postmark and reCAPTCHA, the link will be emailed to you instead."]))

(def signin-sent
  (ui/page
   {}
   [:div "We've sent a sign-in link to your email address. Please check your inbox."]))

(def signin-fail
  (ui/page
   {}
   [:div
    "Your sign-in request failed. There are several possible reasons:"]
   [:ul
    [:li "You failed the reCAPTCHA test."]
    [:li "We think your email address is invalid or high risk."]
    [:li "We tried to email the link to you, but there was an unexpected error."]
    [:li "You opened the sign-in link on a different device or browser than the one you requested it on."]]))

(def features
  {:routes [["/auth/send" {:post send-token}]
            ["/auth/verify/:token" {:get verify-token}]
            ["/auth/signout" {:post signout}]]
   :static {"/auth/printed/" signin-printed
            "/auth/sent/" signin-sent
            "/auth/fail/" signin-fail}})
