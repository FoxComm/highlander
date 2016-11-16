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
    [gws.mandrill.api.templates :as templates]
    [helpers.activities-transforms :as at]))


;; mandrill client
(defn client []
  (let [mkey (settings/get :mandrill_key)]
    (when (empty? mkey)
      (throw (ex-info "Mandrill key is not defined" {})))
    (client/create mkey)))

;; mailchimp client
(defn mclient []
  (let [mkey (settings/get :mailchimp_key)]
    (when (empty? mkey)
      (throw (ex-info "Mailchimp key is not defined" {})))
    (mailchimp/create-client "fox-messaging" mkey)))


(defn make-tpl-vars
  "Convert clojure map to mandtrill template vars"
  [vars]
  {:pre  [(map? vars)]}
  (for [[k v] vars]
   {:name k :content v}))


(defn gen-msg
  [{customer-email :email customer-name :name :as rcpt} vars {:keys [subject text html] :as opts}]
  (let [base-vars {:main_public_domain (settings/get :main_public_domain)
                   :email_subject subject
                   :update_profile_link (settings/get :update_customer_profile_link)
                   :customer_name customer-name}]
   (merge opts {:to
                [rcpt]
                :global_merge_vars (make-tpl-vars (merge base-vars vars))
                :merge_language "handlebars"
                :auto_text true

                :from_email (settings/get :from_email)
                :subject subject})))

(defn send-template!
  [slug template]
  (messages/send-template (client)
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
        order (get data "order")
        email (get-in order ["customer" "email"])
        customer-name (get-in order ["customer" "name"] "")
        order-ref (get order "referenceNumber")
        msg (gen-msg {:email email :name customer-name}
                     {:items (let [skus (get-in order ["lineItems" "skus"])]
                               (map at/sku->item skus))
                      :totals (at/format-prices (get order "totals"))
                      :placed_at (at/date-simple-format (get order "placedAt"))
                      :shipping_method (get-in order ["shippingMethod" "name"])
                      :shipping_address (get order "shippingAddress")
                      :billing_address (get order "billingAddress")
                      :order_ref order-ref}

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
                              {:rewards ""}
                              {:subject (settings/get :order_canceled_subject)})))))

(defmethod handle-activity :user_remind_password
  [activity]
  (let [email (get-in activity [:data "user" "email"])
              reset-code (get-in activity [:data "code"])
              reset-pw-link (format (settings/get :reset_password_link_format) reset-code)
              full-reset-password-link (format "%s/%s" (settings/get :shop_base_url) reset-pw-link)
              customer-name (get-in activity [:data "user" "name"])]
       (send-template! (settings/get :customer_remind_password_template)
           (gen-msg {:email email :name customer-name}
               {:reset_password_link full-reset-password-link
                :reset_code reset-code}
               {:subject (settings/get :customer_remind_password_subject)}))))

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
    (messages/send (client) {:message msg})))

(defn handle-new-customer
  [activity]
  (let [email (get-in activity [:data "user" "email"])
        customer-name (get-in activity [:data "user" "name"] "")
        customer-id (get-in activity [:data "user" "id"])]
    (when (settings/add-new-customers-to-mailchimp?)
      (try
        (mailchimp/create-member-for-list
          (mclient)
          (settings/get :mailchimp_customers_list_id)
         {:email_type "html"
          :email_address email
          :merge_fields {"NAME" customer-name}
          :status "subscribed"})
       (catch Exception e (prn "Can't add user to list" e))))


   (send-template! (settings/get :customer_created_template)
                   (gen-msg {:email email :name customer-name}
                            {}
                            {:subject (settings/get :customer_registration_subject)}))))


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

    (send-template! (settings/get :admin_invitation_template) msg)))
