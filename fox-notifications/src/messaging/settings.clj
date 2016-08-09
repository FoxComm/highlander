(ns messaging.settings
  (:refer-clojure :exclude [get]))

(def schema
  [
    {:name "add_new_customers_to_mailchimp"
     :title "Add new customers to mailchimp list"
     :type "bool"
     :default true}

    {:name "mailchimp_customers_list_id"
     :title "Mailchimp list id for customers"
     :type "string"
     :default ""
     }

    {:name "from_email"
     :title "Email used to from"
     :type "string"
     :default ""
     }
    {:name "order_checkout_text"
     :title "Order checkout mail text template"
     :type "string"
     :default "Your order %s just placed"
     }
    {:name "order_checkout_subject"
     :title "Order Checkout mail subject"
     :type "string"
     :default "Order placed"
     }
    {:name "order_canceled_text"
     :title "Order checkout mail text template"
     :type "string"
     :default "Your order %s has been canceled"
     }
    {:name "order_canceled_subject"
     :title "Order canceled mail subject"
     :type "string"
     :default  "Order cancellation"
     }
    {:name "admin_invintation_subject"
     :title "Admin invintation mail subject"
     :type "string"
     :default "Invintation"
     }
    {:name "customer_invintation_subject"
     :title "Customer invintation mail subject"
     :type "string"
     :default "Registration" ;; fixme
     }
    {:name "mandrill_key"
     :title "Mandrill API key"
     :type "string"
     :default ""
     }
    {:name "mailchimp_key"
     :title "Mailchimp API key"
     :type "string"
     :default ""
     }
    {:name "admin_server_name"
     :title "Admin domain"
     :type "string"
     :default ""
     }
    {:name "retailer_name"
     :title "Retailer name"
     :type "string"
     :default ""
     }
    {:name "slack_webhook_url"
     :title "Slack webhook url"
     :type "string"
     :default ""
     }
    {:name "user_invitation_template"
     :title "User Invitation Mandrill Template"
     :type "string"
     :default "user-invitation"
     }
    {:name "customer_created_template"
     :title "Customer Creation Mandrill Template"
     :type "string"
     :default "customer-created"
     }
    {:name "order_canceled_template"
     :title "Order Canceled Mandrill Template"
     :type "string"
     :default "order-canceled"
     }
    {:name "order_confirmation_template"
     :title "Order Confirmation Mandrill Template"
     :type "string"
     :default "order-confirmation"
     }
    ])

(def store (atom {}))

(defn update-settings
  [new-settings]
  (->> new-settings
       clojure.walk/keywordize-keys
       (reset! store)))

(deref store)

(defn get
  [k]
  (clojure.core/get @store (keyword k)))


(defn add-new-customers-to-mailchimp?
  []
  (get :add_new_customers_to_mailchimp))
