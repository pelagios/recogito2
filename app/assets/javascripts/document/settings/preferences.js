require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([], function() {

  jQuery(document).ready(function() {
        /** Returns the state of a single input DOM node **/
    var getInputState = function(node) {
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

        onChange = function() {
          var setting = toSetting(getState());

          // TODO store via API
          console.log(setting);
        };

    jQuery('#gazetteer-preferences input').change(onChange);
  });

});
