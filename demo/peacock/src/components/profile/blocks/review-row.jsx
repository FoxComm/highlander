
import React from 'react';

const ReviewRow = (props) => {
  const { product, date, status, rating } = props.review;
  return (
    <tr>
      <td>{product}</td>
      <td>{date}</td>
      <td>{status}</td>
      <td>{rating}</td>
    </tr>
  );
};

export default ReviewRow;
