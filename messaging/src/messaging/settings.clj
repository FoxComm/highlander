(ns messaging.settings
  (:refer-clojure :exclude [get])
  (:require clojure.walk))

(def schema
  [
    ;; -- Core mailchimp/Mandrill settings
    {:name "add_new_customers_to_mailchimp"
     :title "Add new customers to mailchimp list"
     :type "bool"
     :default true}

    {:name "mailchimp_customers_list_id"
     :title "Mailchimp list id for customers"
     :type "string"
     :default ""}

    {:name "mandrill_key"
     :title "Mandrill API key"
     :type "string"
     :default ""}

   {:name "mailchimp_key"
    :title "Mailchimp API key"
    :type "string"
    :default ""}

   {:name "from_email"
    :title "From email used in transactional emails"
    :type "string"
    :default ""}
;; -- Other common settings


   {:name "admin_base_url"
    :title "Base URL to admin"
    :type "string"
    :default ""}

   {:name "shop_base_url"
    :title "Base URL to customer site"
    :type "string"
    :default ""}

   {:name "retailer_name"
    :title "Retailer name"
    :type "string"
    :default ""}

   {:name "additional_merge_vars"
    :title "Merge vars"
    :type "string"
    :default ""}
; ----

; ---- Templates settings
   {:name "update_customer_profile_link"
    :title "Link to customer profile"
    :default ""
    :type "string"}
; --- Order templates
   {:name "order_confirmation_template"
    :title "Order Confirmation Mandrill Template"
    :type "string"
    :default "order-confirmation"}

   {:name "order_checkout_subject"
    :title "Order Confirmation mail subject"
    :type "string"
    :default ""}
; - Order cancelation
   {:name "order_canceled_template"
    :title "Order Canceled Mandrill Template"
    :type "string"
    :default "order-canceled"}

   {:name "order_canceled_subject"
    :title "Order canceled mail subject"
    :type "string"
    :default  "Order cancellation"}
; - Order shipped
   {:name "order_shipped_subject"
    :title "Order shipped subject"
    :type "string"
    :default ""}

   {:name "order_shipped_template"
    :title "Order shipped template"
    :type "string"
    :default ""}

; ---

    ; - Gift card by customer
   {:name "gift_card_customer_template"
    :title "Gift card template"
    :type "string"
    :default ""}

   {:name "gift_card_customer_subject"
    :title "Gift card email subject"
    :type "string"
    :default ""}

    ; - Admin invintation
   {:name "admin_invitation_template"
    :title "Admin Invitation Mandrill Template"
    :type "string"
    :default ""}

   {:name "admin_invitation_subject"
    :title "Admin invitation mail subject"
    :type "string"
    :default ""}
; --
    ; {:name "slack_webhook_url"
    ;  :title "Slack webhook url"
    ;  :type "string"
    ;  :default ""}

; --

   {:name "customer_created_template"
    :title "Customer Registration Mandrill Template"
    :type "string"
    :default "customer-created"}

   {:name "customer_registration_subject"
    :title "Customer registration mail subject"
    :type "string"
    :default ""}

; --
   {:name "customer_remind_password_template"
    :title "Customer remind password Mailchimp template"
    :type "string"
    :default ""}

   {:name "customer_remind_password_subject"
    :title "Customer remind password mail subject"
    :type "string"
    :default ""}

   {:name "reset_password_link_format"
    :title "Relative URL to reset password link, use %s to inject reset code"
    :type "string"
    :default ""}])


(defonce store (atom {}))

(defn update-settings
  [new-settings]
  {:pre [(map? new-settings)]}
  (->> new-settings
       clojure.walk/keywordize-keys
       (reset! store)))


(defn get
  [k]
  (clojure.core/get @store (keyword k)))


(defn add-new-customers-to-mailchimp?
  []
  (get :add_new_customers_to_mailchimp))
