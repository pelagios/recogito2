require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require(['common/config'], function(Config) {

  jQuery(document).ready(function() {

    var useAll = jQuery('#use-all'),

        table = jQuery('#gazetteer-list table'),

        gazetteers = jQuery('#gazetteer-list input'),

        plausibilityWarning = jQuery('.plausibility-warning'),

        btnClearVocabulary = jQuery('#clear-vocabulary'),

        inputUploadVocabulary = jQuery('#upload-vocabulary'),

        /** Returns the state of a single input DOM node **/
        getInputState = function(node) {
          var input = jQuery(node),
              checked = input.is(':checked'),
              id = input.closest('tr').data('id'),
              state = {};

          state[id] = checked;
          return state;
        },

        /** Translates the current checkbox state to a JS object **/
        getState = function() {
          var listState = {};

          jQuery('#gazetteer-list input').each(function(idx, n) {
            jQuery.extend(listState, getInputState(n));
          });

          return {
            use_all: jQuery('#use-all').is(':checked'),
            selections: listState
          };
        },

        /** Computes the settings object for the state **/
        toSetting = function(state) {
          var includes = [];

          jQuery.each(state.selections, function(key, val) {
            if (val) includes.push(key);
          });

          return {
            use_all: state.use_all,
            includes: includes
          }
        },

        onToggleUseAll = function(evt) {
          var isChecked = useAll.is(':checked');
          if (isChecked) {
            table.addClass('disabled');
            gazetteers.prop('checked', true);
          } else {
            table.removeClass('disabled');
          }

          onChange(evt);
        },

        setWarning = function(setting) {
          if (setting.use_all || setting.includes.length > 0)
            plausibilityWarning.fadeOut(200);
          else
            plausibilityWarning.show();
        },

        onChange = function(evt) {
          var forId = jQuery(evt.target).attr('id'),
              notifier = jQuery('*[data-for="' + forId + '"]'),
              setting = toSetting(getState());

          setWarning(setting);

          jsRoutes.controllers.document.settings.SettingsController.setGazetteerPreferences(Config.documentId).ajax({
            data: JSON.stringify(setting),
            contentType: 'application/json'
          }).success(function() {
            notifier.show();
            setTimeout(function() { notifier.fadeOut(200); }, 1000);
          }).fail(function(error) {
            console.log('error');
          });
        },

        onClearVocabulary = function() {
          jQuery.ajax({
            url: '/document/' + Config.documentId + '/settings/prefs/tag-vocab',
            type: 'DELETE',
            success: function(result) {
              location.reload();
            }
          })
        },

        onUploadVocabulary = function() {
          var data = new FormData();
          data.append('file', this.files[0]);

          var f = this.files[0];
          var reader = new FileReader();
          
          reader.addEventListener('load', function (e) {
              var rows = e.target.result.split('\n'),
                  tags = [];
                  
              rows.forEach(function(row) {
                var fields = row.split(',').map(function(f) { return f.trim() });
                if (fields[0].length > 0) {
                  if (fields.length == 1) {
                    tags.push(fields[0]);
                  } else if (fields.length > 1) {
                    tags.push({ value: fields[0], uri: fields[1] });
                  }
                }
              });

              // Don't allow mixed tag vocabularies 
              var allStrings = tags.every(function(t) {
                    return typeof t === 'string' || t instanceof String;
                  }),

                  allObjects = tags.every(function(t) {
                    return t.value && t.uri
                  });

              if (allStrings || allObjects) {
                jQuery.ajax({
                  url: '/document/' + Config.documentId + '/settings/prefs/tag-vocab',
                  type: 'POST',
                  contentType: "application/json; charset=utf-8",
                  data: JSON.stringify(tags),
                  success: function(result) {
                    location.reload();
                  }
                });
              } else {
                alert('Mixed vocabularies (with and without URIs) are currently not supported.');
              }
          });

          reader.readAsText(f, 'UTF-8');
        };

    useAll.change(onToggleUseAll);
    gazetteers.change(onChange);

    btnClearVocabulary.click(onClearVocabulary);
    inputUploadVocabulary.change(onUploadVocabulary);

    setWarning(toSetting(getState()));
  });

});
