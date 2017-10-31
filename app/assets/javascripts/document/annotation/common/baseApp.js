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
        preselected,

        scrollIntoView = function(bounds) {
          var scrollTo = bounds.top - jQuery(window).height() + 100;
          if (scrollTo > 0)
            jQuery('html, body').animate({ scrollTop: scrollTo });
        };

    this.header.incrementAnnotationCount(annotations.length);
    // var startTime = new Date().getTime();
    this.highlighter.initPage(annotations);
    // console.log('took ' + (new Date().getTime() - startTime) + 'ms');

    if (urlHash) {
      preselected = this.highlighter.findById(urlHash);
      if (preselected) {
        this.selector.setSelection(preselected);
        scrollIntoView(preselected.bounds);
      }
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

  BaseApp.prototype.upsertAnnotationBatch = function(annotationStubs) {
    var self = this,

        // Finds the original stub that corresponds to the annotation
        findStub = function(annotation) {
          return annotationStubs.find(function(stub) {
            // Determine identity based on the anchor
            return stub.anchor === annotation.anchor;
          });
        };

    self.header.showStatusSaving();
    API.storeAnnotationBatch(annotationStubs)
       .done(function(annotations) {
         // Update header info
         self.header.incrementAnnotationCount(annotations.length);
         self.header.updateContributorInfo(Config.me);
         self.header.showStatusSaved();

         annotations.forEach(function(annotation) {
           // Note: it *should* be safe to assume that the annotations come in the same
           // order as the original stubs, but we'll be a little definsive here, just in case
           var stub = findStub(annotation);
           jQuery.extend(stub, annotation);
           self.highlighter.refreshAnnotation(stub);
         });
       })
       .fail(function(error) {
         self.header.showSaveError();
       });
  };

  BaseApp.prototype.onCreateAnnotation = function(selection) {
    if (selection.isNew)
      this.highlighter.convertSelectionToAnnotation(selection);
    this.upsertAnnotation(selection.annotation);
  };

  BaseApp.prototype.onCreateAnnotationBatch = function(selections) {
    var self = this,
        annotationStubs = selections.map(function(selection) {
          if (selection.isNew)
            self.highlighter.convertSelectionToAnnotation(selection);
          return selection.annotation;
        });
    self.upsertAnnotationBatch(annotationStubs);
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
