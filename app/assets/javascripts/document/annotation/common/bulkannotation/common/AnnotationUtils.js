export class AnnotationUtils {

  static getQuote(annotation) {
    const quoteBody = annotation.bodies.find(b => {
      return b.type == 'QUOTE';
    });
    return (quoteBody) ? quoteBody.value : null;
  }

}
