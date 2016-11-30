(ns helpers.activities-transforms
  (:import [java.util Locale]
           [java.time Instant LocalDateTime ZoneId]))


(defn format-price
  [price]
  (let [float-price (/ price 100.0)]
    (String/format Locale/US "%.2f" (to-array [float-price]))))

(defn format-price-int
  "Format price without fraction with floor"
  [price]
  (let [float-price (Math/floor (/ price 100.0))]
    (String/format Locale/US "%.0f" (to-array [float-price]))))

(defn parse-date
  [^String d]
  (Instant/parse d))

(defn date-simple-format
  "Convert string date to format mm/dd/YYY"
  [^String d]
  (let [ld (-> d
               parse-date
               (LocalDateTime/ofInstant (ZoneId/systemDefault)))]
    (format "%2d/%2d/%d" (.getMonthValue ld) (.getDayOfMonth ld) (.getYear ld))))

(defn format-prices
 ([m price-keys]
  (merge m
         (into {}
               (map (fn [[k v]] [k (format-price v)])
                    (select-keys m price-keys)))))
 ([m]
  (format-prices m (keys m))))


(defn sku->item
  [sku]
  (-> sku
      (select-keys ["imagePath" "name" "quantity" "totalPrice" "price"])
      (format-prices ["price" "totalPrice"])))
