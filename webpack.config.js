'use strict';

var webpack = require('webpack'),
    jsPath  = 'app/assets/javascripts',
    path = require('path'),
    srcPath = path.join(__dirname, 'app/assets/javascripts');

var config = {
    target: 'web',
    entry: {
      gazetteers: path.join(srcPath, 'admin/gazetteers/app.jsx')
    },
    output: {
        path:path.resolve(__dirname, jsPath, '../build'),
        publicPath: '',
        filename: '[name].js'
    },
    module: {
        rules: [
            {
                test: /\.jsx?$/,
                exclude: /node_modules/,
                use: {
                  loader: 'babel-loader',
                  options: {
                    presets: ['react']
                  }
                }
            }
        ]
    }
};

module.exports = config;
