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
      applyToAnnotated  : true,
      applyToUnannotated: true,
      mergePolicy       : 'APPEND',
    };
  }

  computeMatches() {
    const computeAnnotated = () => {
      const filtered = this.props.annotations
        .filterByQuote(this.props.quote)
        .removeById(this.props.original.annotation_id);

      return (this.state.applyIfStatus) ?
        filtered.filterByVerificationStatus(this.state.applyIfStatus).length :
        filtered.length;
    }

    const annotated = (this.props.mode == 'DELETE' || this.state.applyToAnnotated) ? computeAnnotated() : 0;
    const unannotated = (this.state.applyToUnannotated && !this.state.applyIfStatus) ?
      this.props.unannotatedMatches : 0;

    const total = annotated + unannotated;

    return {
      annotated: annotated,
      unannotated: unannotated,
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

  onChangeApplyIf(status) {
    this.setState({ applyIfStatus: status });
  }

  onChangeApplyTo(diff) {
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
          onChange={this.onChangeApplyIf.bind(this)} />

        {this.props.mode == 'REAPPLY' &&
          <ApplyTo
            disableUnannotated={this.props.unannotatedMatches == 0 || this.state.applyIfStatus}
            applyToAnnotated={this.state.applyToAnnotated}
            applyToUnannotated={this.state.applyToUnannotated}
            onChange={this.onChangeApplyTo.bind(this)} />
        }

        <MatchSummary
          mode={this.props.mode}
          total={stats.total}
          annotated={stats.annotated}
          unannotated={stats.unannotated} />

        {this.props.mode == 'DELETE' &&
          <NoAdminWarning visible={true} />
        }

        {this.props.mode == 'REAPPLY' &&
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
