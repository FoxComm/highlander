(ns messaging.mail
  (:require
    [aleph.http :as http]
    [pjson.core :as json]
    [byte-streams :as bs]
    [clojure.string :as string]
    [environ.core :refer [env]]
    [clojchimp.client :as mailchimp]
    [gws.mandrill.client :as client]
    [gws.mandrill.api.messages :as messages]
    [gws.mandrill.api.templates :as templates]))


(def templates {:order-confirmation "fc-order-confirmation"
                :order-canceled "fc-order-cancellation"
                :customer-created "fc-customer-created"
                :user-invintation "new-user-invitation"})



;; env settings
(def mandrill-key (delay (:mandrill-key env)))
(def from_email (delay (:from-email env)))
(def base-domain (delay (:admin-server-name env)))
(def retailer-name (delay (:retailer-name env)))

;; TODO: move to new settings
(def add-new-customers-to-mailchimp (delay (->> (:add-new-customers-to-mailchimp env)
                                            string/lower-case
                                            (contains? #{"t" "true" "y" "yes" "1" "on"}))))
(def mailchimp-customers-list-id (delay (:mailchimp-customers-list-id env)))


(contains?  #{"t" "true" "y" } "tdrue")
;; mandrill client
(def client (delay (client/create @mandrill-key)))
;; mailchimp client
(def mclient (delay
              (mailchimp/create-client "fox-messaging"
                                       (:mailchimp-api-key env))))




(defn make-tpl-vars
  "Convert clojure map to mandtrill template vars"
  [vars]
  {:pre  [(map? vars)]}
  (for [[k v] vars]
   {:name k :content v}))


(defn gen-msg
  [{customer-email :email customer-name :name :as rcpt} vars {:keys [subject text html] :as opts}]
  (let [base-vars {:fc_domain @base-domain}]
   (merge opts {:to
                [rcpt]
                :global_merge_vars (make-tpl-vars (merge base-vars vars))

                :from_email @from_email
                :subject subject
                :text text})))

(defn send-template!
  [slug template]
  (messages/send-template @client
                          {:template_name slug
                           :template_content []
                           :message template}))


(defn dispatch-activity
  [activity]
  (keyword (:activity_type activity)))


(defmulti handle-activity dispatch-activity)
(defmethod handle-activity :default [act] nil)

(defmethod handle-activity :order_checkout_completed
  [activity]
  (let [data (:data activity)
        email (get-in data ["order" "customer" "email"])
        customer-name (get-in data ["order" "customer" "name"])
        order-ref (get-in data ["order" "referenceNumber"])
        msg (gen-msg {:email email :name customer-name}
                     {:message (format "Your order %s just placed" order-ref)
                      :rewards ""}
                     {:subject "Order confirmation"})]
    (send-template! (:order-confirmation templates) msg)))

(defmethod handle-activity :order_state_changed
  [activity]
  (let [data (:data activity)
        email (get-in data ["order" "customer" "email"])
        customer-name (get-in data ["order" "customer" "name"])
        order-ref (get-in data ["order" "referenceNumber"])
        new-state (get-in data ["order" "orderState"])]
   (when (= "canceled" new-state)
     (send-template! (:order-canceled templates)
                     (gen-msg {:email email :name customer-name}
                              {:message (format "Your order %s has been canceled" order-ref)
                               :rewards ""}
                              {:subject "Order cancellation"})))))

(defmethod handle-activity :send_simple_mail
  [activity]
  (let [email (get-in activity [:data "email"])
        customer-name (get-in activity [:data "name"])
        msg (gen-msg {:email email :name customer-name}
                     {}
                     (merge {:text (get-in activity [:data "text"])
                             :html (get-in activity [:data "html"])
                             :subject (get-in activity [:data "subject"])}

                            (get-in activity [:data "opts"])))]
    (messages/send @client {:message msg})))

(defmethod handle-activity :customer_created
  [activity]
  (let [email (get-in activity [:data "customer" "email"])
        customer-name (get-in activity [:data "customer" "name"])
        customer-id (get-in activity [:data "customer" "id"])
        reset-password-link (str "http://" @base-domain "/reset-password/" customer-id)]
    (when @add-new-customers-to-mailchimp
      (try
        (mailchimp/create-member-for-list
          @mclient
          @mailchimp-customers-list-id
          {:email_type "html"
           :email_address email
           :merge_fields {"NAME" customer-name}
           :status "subscribed"})
        (catch Exception e (prn "Can't add user to list" e))))


    (send-template! (:customer-created templates)
                    (gen-msg {:email email :name customer-name}
                             {:reset_password_link reset-password-link
                              :customer_name customer-name
                              :rewards ""}
                             {:subject "Customer created"}))))

(defmethod handle-activity :store_admin_created
  ;; TODO: change type of activity when phoenix will be updated
  [activity]
  (let [data (:data activity)
        email (get-in data ["storeAdmin" "email"])
        new-admin-name (get-in data ["storeAdmin" "name"])
        store-admin-name (get-in data ["admin" "admin" "name"])
        msg (gen-msg {:email email :name new-admin-name}
                     {:user_being_invited new-admin-name
                      :name_of_retailer @retailer-name ;; move to settings
                      :user_that_invited_you store-admin-name}

                     {:subject "Invintation"})]

    (send-template! (:user-invintation templates) msg)))
