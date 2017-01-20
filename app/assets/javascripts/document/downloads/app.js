require.config({
  baseUrl: "/assets/javascripts/",
  fileExclusionRegExp: /^lib$/
});

require([
  'common/ui/modal',
  'common/config'
], function(Modal, Config) {

  var STORAGE_LOCATION = 'r2.doc-' + Config.documentId + '.downloads.settings';

  var Settings = function(strOrObj) {

    var settings = (jQuery.isPlainObject(strOrObj)) ? strOrObj : JSON.parse(strOrObj),

        isValid = function() {
          return settings.id !== undefined && settings.title !== undefined;
        },

        asObj = function() {
          return settings;
        },

        asString = function() {
          return JSON.stringify(settings);
        },

        get = function(key) {
          return settings[key];
        };

    this.isValid = isValid;
    this.asObj = asObj;
    this.asString = asString;
    this.get = get;
  };

  var SettingsModal = function(fields, opt_settings) {
    var self = this,

        options =
          '<option></option>' + // Empty option
          fields.map(function(name, idx) {
            return '<option value="' + idx + '">' + name + '</option>';
          }).join(''),

        body = jQuery(
          '<div>' +
            '<form class="crud">' +
              '<div class="validation-error" style="display:none;"><span class="icon">&#xf00d;</span> Please configure ' +
                'the fields marked as Required.</div>' +

              '<p class="instructions">Configure the columns that correspond to gazetteer ' +
                'properties. Unmapped columns will be exported as generic GeoJSON properties.</p>' +

              // TODO base URL

              '<dl id="id">' +
                '<dt><label for="id">Unique ID</label></dt>' +
                '<dd><select>' + options + '</select></dd>' +
                '<dd class="info">Required</dd>' +
              '</dl>' +

              '<dl id="title">' +
                '<dt><label for="title">Title</label></dt>' +
                '<dd><select>' + options + '</select></dd>' +
                '<dd class="info">Required</dd>' +
              '</dl>' +

              '<dl class="name">' +
                '<dt><label>Name</label></dt>' +
                '<dd><select>' + options + '</select></dd>' +
              '</dl>' +

              '<dl class="add-name">' +
                '<dt></dt>' +
                '<dd><span class="icon">&#xf055;</span> ' +
                  '<span class="label">Add Name column</span></dd>' +
              '</dl>' +

              '<dl id="description">' +
                '<dt><label for="title">Description</label></dt>' +
                '<dd><select>' + options + '</select></dd>' +
              '</dl>' +

              '<dl id="country">' +
                '<dt><label for="country">Country Code</label></dt>' +
                '<dd><select>' + options + '</select></dd>' +
              '</dl>' +

              '<dl id="latitude">' +
                '<dt><label for="latitude">Latitude</label></dt>' +
                '<dd><select>' + options + '</select></dd>' +
              '</dl>' +

              '<dl id="longitude">' +
                '<dt><label for="longitude">Longitude</label></dt>' +
                '<dd><select>' + options + '</select></dd>' +
              '</dl>' +

              // TODO advanced geometry

              '<div class="buttons">' +
                '<button type="submit" class="btn">OK</button>' +
                '<button class="btn outline cancel">Cancel</button>' +
              '</div>' +
            '</form>' +
          '</div>'),


        /** Restores the settings (if any) from the local store **/
        restoreSettings = function() {
          var setIfDefined = function(key, input) {
                var val = opt_settings.get(key);
                if (val !== undefined)
                  input.val(val);
              };

          if (opt_settings) {
            setIfDefined('id', body.find('#id select'));
            setIfDefined('title', body.find('#title select'));
            setIfDefined('name', body.find('.name select'));
            setIfDefined('description', body.find('#description select'));
            setIfDefined('country', body.find('#country select'));
            setIfDefined('latitude', body.find('#latitude select'));
            setIfDefined('longitude', body.find('#longitude select'));
          }
        },

        /** Persists the current settings to the local store **/
        getSettings = function() {
          var parseNumber = function(str) {
                if (str.trim().length === 0) return undefined;
                else return parseInt(str);
              };

          return new Settings({
            'id': parseNumber(body.find('#id select').val()),
            'title': parseNumber(body.find('#title select').val()),
            'name': parseNumber(body.find('.name select').val()),
            'description': parseNumber(body.find('#description select').val()),
            'country': parseNumber(body.find('#country select').val()),
            'latitude': parseNumber(body.find('#latitude select').val()),
            'longitude': parseNumber(body.find('#longitude select').val())
          });
        },

        init = function() {
          body.find('form').submit(onSubmit);
          body.find('button.cancel').click(onCancel);
          restoreSettings();
        },

        onSubmit = function() {
          var settings = getSettings();

          if (settings.isValid()) {
            localStorage.setItem(STORAGE_LOCATION, settings.asString());
            self.fireEvent('ok', settings);
            self.destroy();
          } else {
            body.find('.validation-error').show();
            jQuery('.modal-header').addClass('validation-error'); // Style tweak
          }

          return false;
        },

        onCancel = function() {
          self.destroy();
          return false;
        };

    Modal.apply(this, [ 'Gazetteer Export Settings', body, 'settings' ]);

    init();
    self.open();
  };
  SettingsModal.prototype = Object.create(Modal.prototype);

  jQuery(document).ready(function() {

        // We only need the header fields, but fetch first 5 so that
        // Papa Parse can do proper delimiter guessing
    var CSV_SNIPPET_SIZE = 5,

        // For lack of a better option, we pull header fields from the first part *only*,
        // It's in the responsibility of the user if they want deal with a mix of tables with
        // different schemas. Recogito will only support the base case.
        dataURL = jsRoutes.controllers.document.DocumentController
          .getDataTable(Config.documentId, Config.dataPartSequenceNo[0], CSV_SNIPPET_SIZE).absoluteURL(),

        payloadField = jQuery('.gazetteer #json'),

        btnSettings = jQuery('.gazetteer .settings'),
        btnDownload = jQuery('.gazetteer .download'),

        fields = [], // to be populated from the CSV snippet

        storedSettings = (function() {
          var serialized = localStorage.getItem(STORAGE_LOCATION);
          return (serialized) ? new Settings(serialized) : false;
        })(),

        init = function(csv) {
          refreshSettingsButton(storedSettings);
          fields = csv.meta.fields;
          btnSettings.click(openSettings);
        },

        refreshSettingsButton = function(settings) {
          if (settings && settings.isValid()) {
            btnSettings.removeClass('orange');
            btnSettings.addClass('outline');
            btnSettings.find('.icon').html('&#xf00c;');

            btnDownload.prop('disabled', false);
            payloadField.val(settings.asString());
          }
        },

        openSettings = function() {
          var modal = new SettingsModal(fields, storedSettings);
          modal.on('ok', function(settings) {
            storedSettings = settings;
            refreshSettingsButton(settings);
          });
          return false;
        };

    Papa.parse(dataURL, {
      download : true,
      header   : true, // TODO can we make this configurable through extra table meta?

      // TODO we're not doing an error handling at the moment, but just settings unpopulated
      complete : init
    });

  });

});
