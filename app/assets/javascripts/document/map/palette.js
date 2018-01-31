define([], function() {

  var getRandom = function(scale) {
    var idx = Math.floor(Math.random() * scale.length);
    return scale[idx];
  };

  return {

    scale_ten: [
      '#1f77b4',
      '#ff7f0e',
      '#2ca02c',
      '#d62728',
      '#9467bd',
      '#8c564b',
      '#e377c2',
      '#7f7f7f',
      '#bcbd22',
      '#17becf'
    ],

    scale_ten_pairs: [
      { dark: '#1f77b4', light: '#aec7e8' },
      { dark: '#ff7f0e', light: '#ffbb78' },
      { dark: '#2ca02c', light: '#98df8a' },
      { dark: '#9467bd', light: '#c5b0d5' },
      { dark: '#d62728', light: '#ff9896' },
      { dark: '#8c564b', light: '#c49c94' },
      { dark: '#e377c2', light: '#f7b6d2' },
      { dark: '#7f7f7f', light: '#c7c7c7' },
      { dark: '#bcbd22', light: '#dbdb8d' },
      { dark: '#17becf', light: '#9edae5' }
    ],

    getRandom: function() {
      return getRandom(this.scale_ten);
    },

    getRandomPair: function() {
      return getRandom(this.scale_ten_pairs);
    }

  };

});
