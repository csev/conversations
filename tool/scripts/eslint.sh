#!/bin/bash

jsdir="`dirname '$0'`/../tool/src/webapp/js"

for js in "${jsdir}/"*.js; do
    basename=`basename "$js"`

    if [ "$basename" = "ckeditor-inline-12.1.0.js" ] || [ "$basename" = "vue.min.js" ]; then
        continue;
    fi

    eslint --format unix --config "$jsdir/.eslintrc.json" "$js" ${1+"$@"}
done
