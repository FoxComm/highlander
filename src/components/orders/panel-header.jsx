import React, { PropTypes } from 'react';

const PanelHeader = props => {
	const { isCart, status, text } = props;
  const icon = isCart 
    ? <div className={`fc-orders-panel-header__icon _${status}`}><i className={`icon-${status}`} /></div> : null;

	return (
		<div className="fc-orders-panel-header">
			{icon}
			{text}
		</div>
	);
};

PanelHeader.propTypes = {
	isCart: PropTypes.bool,
	status: PropTypes.string,
	text: PropTypes.string.isRequired
};

PanelHeader.defaultProps = {
	isCart: false,
	status: 'success'
};

export default PanelHeader;
