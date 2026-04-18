(ns clojure-template.unit.presentation.correlation-id-test
    "Unit tests for the correlation-id middleware."
    (:require [clojure.test :refer [deftest is]]
              [clojure-template.presentation.middlewares.correlation-id :as sut])
    (:import [java.util UUID]))

(defn- echo-handler [req]
       {:status 200 :body nil :headers {} :injected-cid (:correlation-id req)})

(deftest uses-existing-correlation-id
         (let [cid     "my-existing-cid"
               request {:headers {"x-correlation-id" cid}}
               handler (sut/wrap-correlation-id echo-handler)
               resp    (handler request)]
              (is (= cid (get-in resp [:headers "X-Correlation-ID"])))))

(deftest generates-correlation-id-when-absent
         (let [request {:headers {}}
               handler (sut/wrap-correlation-id echo-handler)
               resp    (handler request)]
              (is (some? (get-in resp [:headers "X-Correlation-ID"])))))

(deftest injects-correlation-id-into-request
         (let [cid     "injected-cid"
               request {:headers {"x-correlation-id" cid}}
               handler (sut/wrap-correlation-id echo-handler)
               resp    (handler request)]
              (is (= cid (:injected-cid resp)))))

(deftest generated-id-is-valid-uuid
         (let [request {:headers {}}
               handler (sut/wrap-correlation-id echo-handler)
               resp    (handler request)
               cid     (get-in resp [:headers "X-Correlation-ID"])]
              (is (uuid? (UUID/fromString cid)))))
