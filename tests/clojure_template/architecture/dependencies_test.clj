(ns clojure-template.architecture.dependencies-test
    "Validates layering: domain has no upward imports."
    (:require [clojure.test :refer [deftest is testing]]
              [clojure.java.io :as io]
              [clojure.string :as str]
              [clojure.tools.namespace.file :as ns-file]
              [clojure.tools.namespace.parse :as ns-parse]))

(defn ns-requires
      "Returns the set of required namespaces declared in a .clj source file."
      [file]
      (try
       (if-let [ns-decl (ns-file/read-file-ns-decl file)]
               (ns-parse/deps-from-ns-decl ns-decl)
               #{})
       (catch Exception _ #{})))

(defn clj-files-under [dir]
      (->> (file-seq (io/file dir))
           (filter #(.endsWith (.getName %) ".clj"))))

(deftest domain-has-no-application-imports
         (testing "No domain file imports application namespaces"
                  (let [domain-files (clj-files-under "src/clojure_template/domain")]
                       (doseq [f domain-files]
                              (let [reqs (ns-requires f)]
                                   (is (empty? (filter #(str/starts-with? (str %) "clojure-template.application") reqs))
                                       (str (.getPath f) " must not import application layer")))))))

(deftest domain-has-no-infrastructure-imports
         (testing "No domain file imports infrastructure namespaces"
                  (let [domain-files (clj-files-under "src/clojure_template/domain")]
                       (doseq [f domain-files]
                              (let [reqs (ns-requires f)]
                                   (is (empty? (filter #(str/starts-with? (str %) "clojure-template.infrastructure") reqs))
                                       (str (.getPath f) " must not import infrastructure layer")))))))

(deftest domain-has-no-presentation-imports
         (testing "No domain file imports presentation namespaces"
                  (let [domain-files (clj-files-under "src/clojure_template/domain")]
                       (doseq [f domain-files]
                              (let [reqs (ns-requires f)]
                                   (is (empty? (filter #(str/starts-with? (str %) "clojure-template.presentation") reqs))
                                       (str (.getPath f) " must not import presentation layer")))))))

(deftest application-has-no-infrastructure-imports
         (testing "No application file imports infrastructure namespaces"
                  (let [app-files (clj-files-under "src/clojure_template/application")]
                       (doseq [f app-files]
                              (let [reqs (ns-requires f)]
                                   (is (empty? (filter #(str/starts-with? (str %) "clojure-template.infrastructure") reqs))
                                       (str (.getPath f) " must not import infrastructure layer")))))))

(deftest presentation-has-no-domain-imports
         (testing "No presentation file imports domain namespaces directly"
                  (let [pres-files (clj-files-under "src/clojure_template/presentation")]
                       (doseq [f pres-files]
                              (let [reqs (ns-requires f)]
                                   (is (empty? (filter #(str/starts-with? (str %) "clojure-template.domain") reqs))
                                       (str (.getPath f) " must not import domain layer directly")))))))

(deftest infrastructure-has-no-presentation-imports
         (testing "No infrastructure file imports presentation namespaces"
                  (let [infra-files (clj-files-under "src/clojure_template/infrastructure")]
                       (doseq [f infra-files]
                              (let [reqs (ns-requires f)]
                                   (is (empty? (filter #(str/starts-with? (str %) "clojure-template.presentation") reqs))
                                       (str (.getPath f) " must not import presentation layer")))))))
