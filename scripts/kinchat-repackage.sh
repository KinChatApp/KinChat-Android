#!/usr/bin/env bash
# Sourced helper for the KinChat structure migration.
# move_file "old/relative/Path.kt" "new/relative/Path.kt"  -> physically moves the file
# fix_refs  "old/relative/Path.kt" "new/relative/Path.kt"  -> fixes its package line + every FQN reference to it

BASE="app/src/main/java/com/kinchat/app"
SRC_ROOT="app/src/main/java"

move_file() {
  local old="$1" new="$2"
  mkdir -p "$BASE/$(dirname "$new")"
  git mv "$BASE/$old" "$BASE/$new"
}

fix_refs() {
  local old="$1" new="$2"
  local class old_pkg new_pkg old_pkg_esc matches
  class=$(basename "$new" .kt)
  old_pkg="com.kinchat.app.$(dirname "$old" | tr '/' '.')"
  new_pkg="com.kinchat.app.$(dirname "$new" | tr '/' '.')"
  old_pkg_esc=$(printf '%s' "$old_pkg" | sed 's/\./\\./g')

  # fix this file's own package declaration
  sed -i "s/^package ${old_pkg_esc}\$/package ${new_pkg}/" "$BASE/$new"

  # fix every other file that references this exact class by fully-qualified name
  matches=$(grep -rl "${old_pkg}\.${class}" "$SRC_ROOT" 2>/dev/null)
  if [ -n "$matches" ]; then
    echo "$matches" | xargs sed -i "s/${old_pkg_esc}\.${class}/${new_pkg}.${class}/g"
  fi
}
