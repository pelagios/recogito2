import React, { Component } from 'react';
import { CSSTransition } from 'react-transition-group';

import ApplyIf from './sections/ApplyIf.jsx';
import ApplyTo from './sections/ApplyTo.jsx';
import MatchSummary from './sections/MatchSummary.jsx';
import HowToMerge from './sections/HowToMerge.jsx';
import NoAdminWarning from './sections/NoAdminWarning.jsx';

import SelectableOption from './SelectableOption.jsx';
import Footer from './Footer.jsx';

export default class OptionsPane extends Component {

  constructor(props) {
    super(props);

    this.state = {
      applyIfStatus     : null, // Status value or null
      applyIfMatchType  : 'FULL_WORD',
      applyToAnnotated  : true,
      applyToUnannotated: true,
      mergePolicy       : 'APPEND',
    };
  }

  computeMatches() {
    // Existing annotations "potentially available" for reapply -
    // we use this to disable the checkbox and 'how to merge' section in
    // case available = 0
    const availableAnnotated = this.props.annotations
      .filterByQuote(this.props.quote)
      .removeById(this.props.original.annotation_id);

    // Computes no. of annotations actually affected by reapply settings
    const computeAnnotated = () => {
      return (this.state.applyIfStatus) ?
        availableAnnotated.filterByVerificationStatus(this.state.applyIfStatus).length :
        availableAnnotated.length;
    }

    // No. of existing annotations affected by current settings
    const numAnnotated = (this.props.mode == 'DELETE' || this.state.applyToAnnotated) ? computeAnnotated() : 0;

    // Potentially available unannotated matches
    const numAvailableUnannotated = (this.props.phraseCounter) ?
      this.props.phraseCounter(this.props.quote, this.state.applyIfMatchType == 'FULL_WORD') : 0;

    // No. of unannotated match affected by current settings
    const numUnannotated =
      (this.state.applyToUnannotated && !this.state.applyIfStatus) ?
        numAvailableUnannotated : 0;

    // No. of total matches affected by current settings
    const total = numAnnotated + numUnannotated;

    return {
      hasAvailableAnnotated: availableAnnotated.length > 0,
      hasAvailableUnannotated: numAvailableUnannotated > 0,
      numAnnotated: numAnnotated,
      numUnannotated: numUnannotated,
      total: total
    };
  }

  mergeState(diff) {
    this.setState(prev => {
      const next = {};
      Object.assign(next, prev, diff);
      return next;
    });
  }

  onChangeProperty(diff) {
    this.setState(diff);
  }

  onChangeMergePolicy(value) {
    this.setState({ mergePolicy: value });
  }

  render() {
    const stats = this.computeMatches();

    return(
      <div className="options-pane">
        <ApplyIf
          mode={this.props.mode}
          quote={this.props.quote}
          status={this.state.applyIfStatus}
          matchType={this.state.applyIfMatchType}
          onChange={this.onChangeProperty.bind(this)} />

        {this.props.mode == 'REAPPLY' &&
          <ApplyTo
            applyToAnnotated={this.state.applyToAnnotated}
            disableAnnotated={!stats.hasAvailableAnnotated}

            applyToUnannotated={this.state.applyToUnannotated}
            disableUnannotated={!stats.hasAvailableUnannotated}
            onChange={this.onChangeProperty.bind(this)} />
        }

        <MatchSummary
          mode={this.props.mode}
          total={stats.total}
          annotated={stats.numAnnotated}
          unannotated={stats.numUnannotated} />

        {this.props.mode == 'DELETE' &&
          <NoAdminWarning visible={true} />
        }

        {this.props.mode == 'REAPPLY' && stats.hasAvailableAnnotated &&
          <CSSTransition
            classNames="how-to-merge"
            in={this.state.applyToAnnotated}
            timeout={200}>
            <HowToMerge
              value={this.state.mergePolicy}
              onChange={this.onChangeMergePolicy.bind(this)} />
          </CSSTransition>
        }

        <Footer
          mode={this.props.mode}
          onOk={this.props.onOk.bind(this, this.state)}
          onCancel={this.props.onCancel} />
      </div>
    )
  }

}
