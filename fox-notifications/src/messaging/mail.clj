(ns messaging.mail
  (:require
    [aleph.http :as http]
    [pjson.core :as json]
    [byte-streams :as bs]
    [environ.core :refer [env]]
    [clojure.string :as string]
    [messaging.settings :as settings]
    [clojchimp.client :as mailchimp]
    [gws.mandrill.client :as client]
    [gws.mandrill.api.messages :as messages]
    [gws.mandrill.api.templates :as templates]))


(def admin_server_name (delay (:admin_server_name env)))

;; mandrill client
(def client (delay
              (client/create (settings/get :mandrill_key))))
;; mailchimp client
(def mclient (delay
              (mailchimp/create-client "fox-messaging"
                                       (settings/get :mailchimp_key))))



(defn make-tpl-vars
  "Convert clojure map to mandtrill template vars"
  [vars]
  {:pre  [(map? vars)]}
  (for [[k v] vars]
   {:name k :content v}))


(defn gen-msg
  [{customer-email :email customer-name :name :as rcpt} vars {:keys [subject text html] :as opts}]
  (let [base-vars {:fc_domain @admin_server_name}]
   (merge opts {:to
                [rcpt]
                :global_merge_vars (make-tpl-vars (merge base-vars vars))

                :from_email (settings/get :from_email)
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
        customer-name (get-in data ["order" "customer" "name"] "")
        order-ref (get-in data ["order" "referenceNumber"])
        msg (gen-msg {:email email :name customer-name}
                     {:message (format (settings/get :order_checkout_text) order-ref)
                      :rewards ""}
                     {:subject (settings/get :order_checkout_subject)})]
    (send-template! (settings/get :order_confirmation_template) msg)))

(defmethod handle-activity :order_state_changed
  [activity]
  (let [data (:data activity)
        email (get-in data ["order" "customer" "email"])
        customer-name (get-in data ["order" "customer" "name"] "")
        order-ref (get-in data ["order" "referenceNumber"])
        new-state (get-in data ["order" "orderState"])]
   (when (= "canceled" new-state)
     (send-template! (settings/get :order_canceled_template)
                     (gen-msg {:email email :name customer-name}
                              {:message (format (settings/get :order_canceled_text) order-ref)
                               :rewards ""}
                              {:subject (settings/get :order_canceled_subject)})))))

(defmethod handle-activity :send_simple_mail
  [activity]
  (let [email (get-in activity [:data "email"])
        customer-name (get-in activity [:data "name"] "")
        msg (gen-msg {:email email :name customer-name}
                     {}
                     (merge {:text (get-in activity [:data "text"])
                             :html (get-in activity [:data "html"])
                             :subject (get-in activity [:data "subject"])}

                            (get-in activity [:data "opts"])))]
    (messages/send @client {:message msg})))

(defn handle-new-customer
  [activity]
  (let [email (get-in activity [:data "customer" "email"])
        customer-name (get-in activity [:data "customer" "name"] "")
        customer-id (get-in activity [:data "customer" "id"])
        reset-password-link (str "http://" @admin_server_name "/reset-password/" customer-id)]
    (when (settings/add-new-customers-to-mailchimp?)
      (try
        (mailchimp/create-member-for-list
          @mclient
          (settings/get :mailchimp_customers_list_id)
         {:email_type "html"
          :email_address email
          :merge_fields {"NAME" customer-name}
          :status "subscribed"})
       (catch Exception e (prn "Can't add user to list" e))))


   (send-template! (settings/get :customer_created_template)
                   (gen-msg {:email email :name customer-name}
                            {:reset_password_link reset-password-link
                             :customer_name customer-name
                             :rewards ""}
                            {:subject (settings/get :customer_invintation_subject)}))))


(defmethod handle-activity :customer_registered
  [activity]
  (handle-new-customer activity))

(defmethod handle-activity :customer_created
  [activity]
  (handle-new-customer activity))

(defmethod handle-activity :store_admin_created
  ;; TODO: change type of activity when phoenix will be updated
  [activity]
  (let [data (:data activity)
        email (get-in data ["storeAdmin" "email"])
        new-admin-name (get-in data ["storeAdmin" "name"])
        store-admin-name (get-in data ["admin" "admin" "name"])
        msg (gen-msg {:email email :name new-admin-name}
                     {:user_being_invited new-admin-name
                      :name_of_retailer (settings/get :retailer_name) ;; move to settings
                      :user_that_invited_you store-admin-name}

                     {:subject (settings/get :admin_invintation_subject)})]

    (send-template! (settings/get :user_invitation_template) msg)))
