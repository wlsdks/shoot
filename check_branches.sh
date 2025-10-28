#!/bin/bash

branches=(
  "fix/add-distributed-lock-friend-chatroom"
  "fix/chatroom-participant-n-plus-1"
  "fix/remove-duplicate-add-friend-relation"
  "fix/message-edit-time-validation"
  "feat/data-consistency-improvements"
  "fix/optimistic-lock-retry-improvement"
  "feat/orphaned-message-cleanup"
)

for branch in "${branches[@]}"; do
  echo "=== Checking $branch ==="
  git checkout $branch 2>&1 | grep "Switched"
  ./gradlew clean build -x test 2>&1 | grep -E "BUILD (SUCCESSFUL|FAILED)" | tail -1
  echo ""
done
