define([], function() {

  var DARK = {
    '#31a354': '#006d2c',

    '#1f77b4': '#135d91',
    '#ff7f0e': '#c65f04',
    '#2ca02c': '#196f19',
    '#9467bd': '#6b4391',
    '#d62728': '#7e0a0a',
    '#8c564b': '#522820',
    '#e377c2': '#a7468a',
    '#7f7f7f': '#5e5e5e',
    '#bcbd22': '#92931b',
    '#17becf': '#099ca9',

    '#267278': '#185257',
    '#65338d': '#461f64',
    '#4470b3': '#2f548c',
    '#d21f75': '#a01d5b',
    '#383689': '#282755',
    '#50aed3': '#3281a2',
    '#48b24f': '#2a7e30',
    '#e57438': '#b75621',
    '#569dd2': '#34668c',
    '#569d79': '#3a7054',
    '#58595b': '#363738',
    '#e4b031': '#ac831f',
    '#84d2f4': '#56a9cb',
    '#cad93f': '#9ca92c',
    '#f5c8af': '#c99579',
    '#9ac483': '#729a5c'
  };

  return {

    DEFAULT_FILL_COLOR : '#31a354',

    DEFAULT_STROKE_COLOR : '#006d2c',

    CATEGORY_10 : [
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

    // From http://intelligaia.com/using-colors-for-data-visualization-with-large-categories.php
    CATEGORY_17 : [
      '#267278',
      '#65338d',
      '#4470b3',
      '#d21f75',
      '#383689',
      '#50aed3',
      '#48b24f',
      '#e57438',
      '#569dd2',
      '#569d79',
      '#58595B',
      '#e4b031',
      '#84d2f4',
      '#cad93f',
      '#f5c8af',
      '#9ac483'
    ],

    /** (Currently) just a lookup, not an actual computation **/
    darker : function(color) {
      var col = DARK[color];
      return (col) ? col : '#000';
    }

  };

});
