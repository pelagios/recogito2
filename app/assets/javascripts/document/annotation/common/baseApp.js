/** Contains common high-level functionality needed for text and image 'app' entrypoints **/
define([
  'common/api',
  'common/config',
  'document/annotation/common/page/header'
], function(API, Config, Header, LoadIndicator) {

  var BaseApp = function(highlighter, selector) {
    this.highlighter = highlighter;
    this.selector = selector;
    this.header = new Header();
  };

  BaseApp.prototype.onAnnotationsLoaded = function(annotations) {
    var urlHash = (window.location.hash) ? window.location.hash.substring(1) : false,
        preselected;

    this.header.incrementAnnotationCount(annotations.length);
    // var startTime = new Date().getTime();
    this.highlighter.initPage(annotations);
    // console.log('took ' + (new Date().getTime() - startTime) + 'ms');

    if (urlHash) {
      preselected = this.highlighter.findById(urlHash);
      if (preselected)
        this.selector.setSelection(preselected);
    }
  };

  BaseApp.prototype.onAnnotationsLoadError = function(annotations) {
    // TODO visual notification
  };

  BaseApp.prototype.upsertAnnotation = function(annotationStub) {
    var self = this;

    self.header.showStatusSaving();
    API.storeAnnotation(annotationStub)
       .done(function(annotation) {
         // Update header info
         self.header.incrementAnnotationCount();
         self.header.updateContributorInfo(Config.me);
         self.header.showStatusSaved();

         // Merge server-provided properties (id, timestamps, etc.) into the annotation
         jQuery.extend(annotationStub, annotation);
         self.highlighter.refreshAnnotation(annotationStub);
       })
       .fail(function(error) {
         self.header.showSaveError(error);
       });
  };

  BaseApp.prototype.onCreateAnnotation = function(selection) {
    if (selection.isNew)
      this.highlighter.convertSelectionToAnnotation(selection);
    this.upsertAnnotation(selection.annotation);
  };

  BaseApp.prototype.onUpdateAnnotation = function(annotationStub) {
    // TODO revert on fail?
    this.highlighter.refreshAnnotation(annotationStub);
    this.upsertAnnotation(annotationStub);
  };

  BaseApp.prototype.onDeleteAnnotation = function(annotation) {
    var self = this;

    // TODO restore when store fails?
    this.highlighter.removeAnnotation(annotation);

    API.deleteAnnotation(annotation.annotation_id)
       .done(function() {
         self.header.incrementAnnotationCount(-1);
         self.header.showStatusSaved();
       })
       .fail(function(error) {
         self.header.showSaveError(error);
       });
  };

  return BaseApp;

});
