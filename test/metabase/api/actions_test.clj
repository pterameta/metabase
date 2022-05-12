(ns metabase.api.actions-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer :all]
   [metabase.api.actions :as api.actions]
   [metabase.driver :as driver]
   [metabase.models.database :refer [Database]]
   [metabase.models.table :refer [Table]]
   [metabase.test :as mt]
   [metabase.util :as u]))

(comment api.actions/keep-me)

(defn mock-requests []
  [{:action       "actions/table/insert"
    :request-body {:table-id (mt/id :venues)
                   :values   {:name "Toucannery"}}
    :expected     {:insert-into "VENUES"
                   :values      {:name "Toucannery"}}}
   {:action       "actions/row/update"
    :request-body {:table-id (mt/id :venues)
                   :pk       {:id 1, :name "Red Medicine"}
                   :values   {:name "Toucannery"}}
    :expected     {:update "VENUES"
                   :set    {:name "Toucannery"}
                   :where  ["and"
                            ["=" "id" 1]
                            ["=" "name" "Red Medicine"]]}}
   {:action       "actions/row/delete"
    :request-body {:table-id (mt/id :venues)
                   :pk       {:id 1}}
    :expected     {:delete-from "VENUES"
                   :where       ["=" "id" 1]}}])

(defn- row-action? [action]
  (str/starts-with? action "actions/row"))

(deftest happy-path-test
  (testing "Make sure it's possible to use known actions end-to-end if preconditions are satisfied"
    (mt/with-temp-copy-of-db
      (mt/with-temporary-setting-values [experimental-enable-actions true]
        (mt/with-temp-vals-in-db Database (mt/id) {:settings {:database-enable-actions true}}
          (doseq [{:keys [action request-body expected]} (mock-requests)]
            (testing action
              (is (= expected
                     (mt/user-http-request :crowberto :post 200 action request-body))))))))))

(deftest feature-flags-test
  (testing "Disable endpoints unless both global and Database feature flags are enabled"
    (doseq [{:keys [action request-body]} (mock-requests)
            enable-global-feature-flag?   [true false]
            enable-database-feature-flag? [true false]]
      (testing action
        (mt/with-temporary-setting-values [experimental-enable-actions enable-global-feature-flag?]
          (mt/with-temp-vals-in-db Database (mt/id) {:settings {:database-enable-actions enable-database-feature-flag?}}
            (cond
              (not enable-global-feature-flag?)
              (testing "Should return a 400 if global feature flag is disabled"
                (is (= "Actions are not enabled."
                       (mt/user-http-request :crowberto :post 400 action request-body))))

              (not enable-database-feature-flag?)
              (testing "Should return a 400 if Database feature flag is disabled."
                (is (re= #"^Actions are not enabled for Database [\d,]+\.$"
                         (mt/user-http-request :crowberto :post 400 action request-body)))))))))))

(driver/register! ::feature-flag-test-driver, :parent :h2)

(defmethod driver/database-supports? [::feature-flag-test-driver :actions]
  [_driver _feature _database]
  false)

(deftest actions-feature-test
  (testing "Only allow actions for drivers that support the `:actions` driver feature. (#22557)"
    (mt/with-temporary-setting-values [experimental-enable-actions true]
      (mt/with-temp* [Database [{db-id :id} {:name     "Birds"
                                             :engine   ::feature-flag-test-driver
                                             :settings {:database-enable-actions true}}]
                      Table    [{table-id :id} {:db_id db-id}]]
        (is (partial= {:message (format "%s Database %d \"Birds\" does not support actions."
                                        (u/qualified-name ::feature-flag-test-driver)
                                        db-id)}
                      (mt/user-http-request :crowberto :post 400 "actions/table/insert"
                                            {:table-id table-id
                                             :values   {:name "Toucannery"}})))))))

(deftest validation-test
  (mt/with-temporary-setting-values [experimental-enable-actions true]
    (mt/with-temp-vals-in-db Database (mt/id) {:settings {:database-enable-actions true}}
      (doseq [{:keys [action]} (mock-requests)]
        (testing action
          (testing "Require `:table-id`"
            (is (= {:errors {:table-id "value must be an integer greater than zero."}}
                   (mt/user-http-request :crowberto :post 400 action))))
          (when (row-action? action)
            (testing "Require `:pk` for row actions"
              (is (= {:errors {:pk "value must be a map."}}
                     (mt/user-http-request :crowberto :post 400 action {:table-id (mt/id :venues)})))
              (testing "`:pk` must be a map"
                (is (= {:errors {:pk "value must be a map."}}
                       (mt/user-http-request :crowberto :post 400 action {:table-id (mt/id :venues), :pk 1})))))))))))

(deftest four-oh-four-test
  (mt/with-temporary-setting-values [experimental-enable-actions true]
    (mt/with-temp-vals-in-db Database (mt/id) {:settings {:database-enable-actions true}}
      (doseq [{:keys [action]} (mock-requests)]
        (testing action
          (testing "404 for unknown Table"
            (is (= "Not found."
                   (mt/user-http-request :crowberto :post 404 action {:table-id Integer/MAX_VALUE, :pk {:id 1}}))))))
      (testing "404 for unknown Table action"
        (is (= "Unknown Table action \"fake\"."
               (mt/user-http-request :crowberto :post 404 "actions/table/fake" {:table-id (mt/id :venues), :pk {:id 1}}))))
      (testing "404 for unknown row action"
        (is (= "Unknown row action \"fake\"."
               (mt/user-http-request :crowberto :post 404 "actions/row/fake" {:table-id (mt/id :venues), :pk {:id 1}})))))))
