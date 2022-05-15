#!/usr/bin/env bb

(ns prepare_commit_msg
  (:require [clojure.java.shell :refer [sh]]
            [clojure.string :refer [trim-newline
                                    includes?
                                    upper-case
                                    split]]))

(def ^:private branches-to-skip #{"master"
                                  "main"
                                  "develop"
                                  "test"})

(def commit-message-file ".git/COMMIT_EDITMSG")
(def commit-message (slurp commit-message-file))

(defn- should-skip-this-branch?
  [branch]
  (contains? branches-to-skip branch))

(defn current-git-branch
  []
  (->> (sh "git" "symbolic-ref" "--short" "HEAD")
       :out
       trim-newline))

(defn jira-ticket-number
  []
  (if-let [s (->> (split (current-git-branch) #"/")
                  last
                  (re-seq #"^[a-zA-Z]+-[0-9]+")
                  first)]
    (upper-case s)
    (throw (AssertionError. "브랜치에 티켓 번호가 없습니다. FEAT-123-blah-blah 같은 이름으로 만들어주세요."))))

(defn include-jira-ticket-number?
  []
  (includes? commit-message (jira-ticket-number)))

(defn combine-ticket-number-with-message
  []
  (format "%s %s" (jira-ticket-number) commit-message))

(defn skip?
  [branch]
  (or (should-skip-this-branch? branch)
      (include-jira-ticket-number?)))

(defn -main
  [& _args]
  (->> (if (skip? (current-git-branch))
         commit-message
         (combine-ticket-number-with-message))
       (spit commit-message-file))
  (System/exit 0))
