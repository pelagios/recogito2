define([], function() {

  var DARK = {
    '#1f77b4': '#135d91',
    '#ff7f0e': '#c65f04',
    '#2ca02c': '#196f19',
    '#9467bd': '#6b4391'
  };

  return {

    DEFAULT_FILL_COLOR : '#31a354',

    DEFAULT_STROKE_COLOR : '#006d2c',

    CATEGORY_SCALE : [
      '#1f77b4',
      '#ff7f0e',
      '#2ca02c',
      '#9467bd',
      '#d62728',
      '#8c564b',
      '#e377c2',
      '#7f7f7f',
      '#bcbd22',
      '#17becf'
    ],

    /** (Currently) just a lookup, not an actual computation **/
    darker : function(color) {
      var col = DARK[color];
      return (col) ? col : '#000';
    }

  };

});
