(ns clojure-template.unit.application.pagination-test
    (:require [clojure.test :refer [deftest is testing]]
              [clojure-template.application.dtos.pagination :as pagination]))

(deftest paginate-assigns-all-fields
         (testing "paginate sets data, total, limit, offset correctly"
                  (let [data   [:a :b :c]
                        result (pagination/paginate data 50 10 0)]
                       (is (= data (:data result)))
                       (is (= 50 (:total result)))
                       (is (= 10 (:limit result)))
                       (is (= 0 (:offset result))))))

(deftest paginate-has-next-when-more-items-remain
         (testing "has-next is true when offset+limit < total"
                  (let [result (pagination/paginate [] 50 10 0)]
                       (is (true? (:has-next result)))))
         (testing "has-next is false when offset+limit >= total"
                  (let [result (pagination/paginate [] 10 10 0)]
                       (is (false? (:has-next result))))))

(deftest paginate-has-previous-when-offset-positive
         (testing "has-previous is true when offset > 0"
                  (let [result (pagination/paginate [] 50 10 10)]
                       (is (true? (:has-previous result)))))
         (testing "has-previous is false when offset is 0"
                  (let [result (pagination/paginate [] 50 10 0)]
                       (is (false? (:has-previous result))))))

(deftest paginate-first-page
         (testing "first page: no previous, has next when more items exist"
                  (let [result (pagination/paginate [:x] 5 2 0)]
                       (is (false? (:has-previous result)))
                       (is (true? (:has-next result))))))

(deftest paginate-last-page
         (testing "last page: has previous, no next"
                  (let [result (pagination/paginate [:x] 10 5 5)]
                       (is (true? (:has-previous result)))
                       (is (false? (:has-next result))))))
