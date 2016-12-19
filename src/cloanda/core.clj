(set! *warn-on-reflection* true)

(ns cloanda.core
    (:require [clj-http.client :as client]
              [clojure.string :as string]
              [clj-json.core :as json])

    (:import [java.io.FilterInputStream]))


(defmacro GET [ calling_url header]
  (list client/get calling_url {:as :json :headers header}))


(defmacro POST
    ([ calling_url header]
     (list client/post calling_url {:as :json :headers header}))
    ([ calling_url form header]
     (list client/post calling_url {:form-params form :as :json :headers header})))


(defmacro DELETE [calling_url header]
    (list client/delete calling_url {:as :json :headers header}))


(defmacro PATCH
    ([calling_url header]
     (list client/patch calling_url {:as :json :headers header}))
    ([calling_url form header]
     (list client/patch calling_url {:as :json :headers header})))



;;;;;;;;;;;;;;global varialbes

(def server_env (hash-map
                        :practice ["https://api-fxpractice.oanda.com" "https://stream-fxpractice.oanda.com"]
                        :production ["https://api-fxtrade.oanda.com" "https://stream-fxtrade.oanda.com/"]))



;;;;;;;;;;;;;
(defn gen-headers [ ^String token ^String datetime-format]
    (let [ auth (str "Bearer " token)] ; "UNIX" or "RFC3339"
     {"Authorization" auth  "X-Accept-Datetime-Format" datetime-format}))



(defn read-stream [^java.io.FilterInputStream x]
  (loop [ r (.read x)
         xs []]
    (if-not (= r 10)
      (recur (.read x) (conj xs r))
      (json/parse-string (string/join "" (map char xs))))))

;;;;;;;;;;;;; Utils
(defn params2query [ p]
  (apply str (for [i p] (str "&" (first i) "=" (second i)))))



(defprotocol instrument_protocol
  (get-instrument-list [x])
  (get-current-price [x cur])
  (get-instrument-history [x cur params]))


(defprotocol account_protocol
  (get-accounts [x])
  (get-account-info [x id])
  (get-account-summary [ x id])
  (get-account-instruments [ x id])
  (patch-account [ x id])
  (get-account-changes [ x id]))


(defprotocol order_protocol
  (create-order [x a_id inst unit side type params])
  (get-orders-by-account [x a_id params])
  (get-order-info [x a_id o_id])
  (get-pending-orders [x a_id])
  (replace-order [x a_id o_id])
  (cancel-order [x a_id o_id]))


(defprotocol trade_protocol
  (get-open-trades [x id])
  (get-trades [x id])
  (get-trade-info [x a_id t_id])
  (update-trade [x a_id t_id params])
  (close-trade [x a_id t_id]))


(defprotocol position_protocol
  (get-position [x a_id])
  (get-open-position [x a_id])
  (get-position-by-inst [x a_id inst])
  (close-position [x a_id inst]))


(defprotocol transaction_protocol
  (get-txn-history [x a_id params])
  (get-txn-info [x a_id t_id])
  (get-txn-range [x a_id params])
  (get-txn-since [x a_id params])
  (get-txn-stream [x a_id]))
  ;(get-account-history [x a_id]))
(defprotocol pricing_protocol
  (get-pricing-inst [x a_id params])
  (get-pricing-stream [x a_id params]))


;(defprotocol forex_lab_protocol
;  (get-calendar [x inst period])
;  (hist-pos-ratios [x inst period])
;  (get-spreads [x inst period])
;  (get-cot [x inst])
;  (get-order-book [x inst period]))
;

;(defprotocol streaming_protocol
;  (rate-stream [x a_id inst])
;  (event-stream [x]))


(defrecord api [ rest_url stream_url header]
  instrument_protocol

  ;(get-current-price [ x cur ]
  ;  (GET (str rest_url "/v1/prices?instruments=" (string/join "%2C" cur)) header))

  (get-instrument-history [ x cur params]
    (let [opt_str (params2query params)]
      (GET (str rest_url "/v3/instrument/" cur "/candles?" opt_str ) header)))


  account_protocol
  (get-accounts [ x]
    (GET (str rest_url "/v3/accounts") header))
  (get-account-info [ x id]
    (GET (str rest_url "/v3/accounts/" id) header))
  (get-account-summary [ x id]
    (GET (str rest_url "/v3/accounts/" id "/summary") header))
  (get-account-instruments [ x id]
    (GET (str rest_url "/v3/accounts/" id "/instruments") header))


  (patch-account [ x id]
    (PATCH (str rest_url "/v3/accounts/" id "/configuration") header))
  (get-account-changes [ x id]
      (GET (str rest_url "/v3/accounts/" id "/changes") header))

  order_protocol
  (create-order [x a_id inst units side type params]
    (let [base_cmd  {:instrument  inst :units units :side side :type type}
          exe_cmd (merge base_cmd params)]
      (POST (str rest_url "/v3/accounts/" a_id "/orders") exe_cmd)))


  (get-orders-by-account [x a_id params]
    (GET (str rest_url "/v3/accounts/" a_id "/orders?" (params2query params) ) header))

  (get-order-info [x a_id o_id]
    (GET (str rest_url "/v3/accounts/" a_id "/orders/" o_id) header))

  (get-pending-orders [x a_id]
      (GET (str rest_url "/v3/accounts/" a_id "/pendingOrders" ) header))


  (replace-order [x a_id o_id]
    (PUT (str rest_url "/v3/accounts/" a_id "/orders/" o_id) header))
  (cancel-order [x a_id o_id]
    (PUT (str rest_url "/v1/accounts/" a_id "/orders/" o_id "/cancel") header))

  trade_protocol
  (get-open-trades [x id]
    (GET (str rest_url "/v3/accounts/" id "/openTrades/") header))
  (get-trades [x id]
    (GET (str rest_url "/v3/accounts/" id "/trades/") header))
  (get-trade-info [x a_id t_id]
    (GET (str rest_url "/v3/accounts/" a_id "/trades/" t_id) header))
  (update-trade [x a_id t_id params]
    (PUT (str rest_url "/v3/accounts/" a_id "/trades/" t_id "/orders") params header))
  (close-trade [x a_id t_id]
    (PUT (str rest_url "/v3/accounts/" a_id "/trades/" t_id "/close") header))

  position_protocol
  (get-position [x a_id]
    (GET (str rest_url "/v3/accounts/" a_id "/positions") header))
  (get-open-position [x a_id]
    (GET (str rest_url "/v3/accounts/" a_id "/openPositions") header))
  (get-position-by-inst [x a_id inst]
    (GET (str rest_url "/v3/accounts/" a_id "/positions/" inst) header))
  (close-position [x a_id inst]
    (PUT (str rest_url "/v3/accounts/" a_id "/positions/" inst "/close") header))

  transaction_protocol
  (get-txn-history [x a_id params]
    (GET (str rest_url "/v3/accounts/" a_id "/transactions" (params2query params) ) header))
  (get-txn-info [x a_id t_id]
    (GET (str rest_url "/v3/accounts/" a_id "/transactions/" t_id) header))
  (get-txn-range [x a_id params]
    (GET (str rest_url "/v3/accounts/" a_id "/transactions/idrange" (params2query params) ) header))
  (get-txn-since [x a_id params]
    (GET (str rest_url "/v3/accounts/" a_id "/transactions/sinceid" (params2query params) ) header))
  (get-txn-stream [x a_id]
        "TBD")
  pricing_protocol
  (get-pricing-inst [x a_id params]
    (GET (str rest_url "/v3/accounts/" a_id "/pricing" (params2query params))))
  (get-pricing-stream [x a_id params]
    (GET (str stream_url "/v3/accounts/" a_id "/pricing/stream" (params2query params)))))
  ;(get-account-history [x a_id]
  ;  (GET (str rest_url "/v1/accounts/" a_id "/alltransactions/") header))

  ;forex_lab_protocol
  ;(get-calendar [x inst period]
  ;  (GET (str rest_url "/labs/v1/calendar?instrument=" inst "&period=" period) header))
  ;(hist-pos-ratios [x inst period]
  ;  (GET (str rest_url "/labs/v1/historical_position_ratios?instrument=" inst "&period=" period) header))
  ;(get-spreads [x inst period]
  ;  (GET (str rest_url "/labs/v1/spreads?instrument=" inst "&period=" period) header))
  ;(get-cot [x inst]
  ;  (GET (str rest_url "/labs/v1/commitments_of_traders?instrument=" inst) header))
  ;(get-order-book [x inst period]
  ;  (GET (str rest_url "/labs/v1/orderbook_data?instrument=" inst "&period=" period) header))

  ;streaming_protocol
  ;(rate-stream [x a_id inst]
  ;  (:body (client/get (str stream_url "/v1/prices?accountId=" a_id "&instruments=" (string/join "%2C" inst)) {:as :stream :headers header})))
;
  ;(event-stream [x]
  ;  (:body (client/get (str stream_url "/v1/events") {:as :stream  :headers header}))))


(defn init-api
  ([server_env df] (api. (first server_env) (second server_env) df)))
