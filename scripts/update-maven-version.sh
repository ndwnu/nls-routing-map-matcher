PS3='Increment major or minor version? '
options=("Major" "Minor")
select selected in "${options[@]}"; do
  case $selected in
  "Major")
    mvn build-helper:parse-version versions:set -DnewVersion=\${parsedVersion.nextMajorVersion}.0.0-SNAPSHOT
    break
    ;;
  "Minor")
    mvn build-helper:parse-version versions:set -DnewVersion=\${parsedVersion.majorVersion}.\${parsedVersion.nextMinorVersion}.0-SNAPSHOT
    break
    ;;
  *) echo "invalid option $REPLY" ;;
  esac
done
