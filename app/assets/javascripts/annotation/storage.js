define([], function() {

  return {

    createAnnotation : function(annotationStub, onSuccess, onError) {
      jQuery.ajax({
        url: '/annotations',
        type: 'POST',
        data: JSON.stringify(annotationStub),
        contentType: 'application/json',
        success: onSuccess,
        error: onError
      });
    }

  };

});
