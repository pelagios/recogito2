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
        };

    useAll.change(onToggleUseAll);
    gazetteers.change(onChange);

    setWarning(toSetting(getState()));
  });

});
