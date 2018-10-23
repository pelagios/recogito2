'use strict';

var webpack = require('webpack'),
    path    = require('path'),
    jsPath  = 'app/assets/javascripts',
    srcPath = path.join(__dirname, jsPath);

module.exports = {
  mode: 'production',
  watchOptions: {
    poll: true
  },
  entry: {
    bulkannotation: path.join(srcPath, 'document/annotation/common/bulkannotation/App.jsx'),
    gazetteers: path.join(srcPath, 'admin/gazetteers/App.jsx')
  },
  output: {
    path:path.resolve(__dirname, jsPath, '../build'),
    publicPath: '',
    filename: '[name].js'
  },
  module: {
    rules: [{
      test: /\.jsx?$/,
      exclude: /node_modules/,
      use: {
        loader: 'babel-loader',
        options: {
          presets: ['react']
        }
      }
    }]
  }
};
