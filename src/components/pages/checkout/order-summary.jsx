
import React from 'react';
import styles from './order-summary.css';

import TermValueLine from 'ui/term-value-line';

const OrderSummary = () => {
  return (
    <div styleName="order-summary">
      <div styleName="title">ORDER SUMMARY</div>
      <table styleName="products-table">
        <thead>
          <tr>
            <th styleName="product-image">ITEM</th>
            <th styleName="product-name" />
            <th styleName="product-qty">QTY</th>
            <th styleName="product-price">PRICE</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td styleName="product-image">
              <img src={`data:image/gif;base64,R0lGODdhMAAwAPAAAAAAAP///ywAAAAAMAAw
                AAAC8IyPqcvt3wCcDkiLc7C0qwyGHhSWpjQu5yqmCYsapyuvUUlvONmOZtfzgFz
                ByTB10QgxOR0TqBQejhRNzOfkVJ+5YiUqrXF5Y5lKh/DeuNcP5yLWGsEbtLiOSp
a/TPg7JpJHxyendzWTBfX0cxOnKPjgBzi4diinWGdkF8kjdfnycQZXZeYGejmJl
ZeGl9i2icVqaNVailT6F5iJ90m6mvuTS4OK05M0vDk0Q4XUtwvKOzrcd3iq9uis
F81M1OIcR7lEewwcLp7tuNNkM3uNna3F2JQFo97Vriy/Xl4/f1cf5VWzXyym7PH
hhx4dbgYKAAA7`}
              />
            </td>
            <td styleName="product-name">LOREM ipsum</td>
            <td styleName="product-qty">1</td>
            <td styleName="product-price">$75.00</td>
          </tr>
          <tr>
            <td styleName="product-image">
              <img src={`data:image/gif;base64,R0lGODdhMAAwAPAAAAAAAP///ywAAAAAMAAw
                AAAC8IyPqcvt3wCcDkiLc7C0qwyGHhSWpjQu5yqmCYsapyuvUUlvONmOZtfzgFz
                ByTB10QgxOR0TqBQejhRNzOfkVJ+5YiUqrXF5Y5lKh/DeuNcP5yLWGsEbtLiOSp
a/TPg7JpJHxyendzWTBfX0cxOnKPjgBzi4diinWGdkF8kjdfnycQZXZeYGejmJl
ZeGl9i2icVqaNVailT6F5iJ90m6mvuTS4OK05M0vDk0Q4XUtwvKOzrcd3iq9uis
F81M1OIcR7lEewwcLp7tuNNkM3uNna3F2JQFo97Vriy/Xl4/f1cf5VWzXyym7PH
hhx4dbgYKAAA7`}
              />
            </td>
            <td styleName="product-name">dfg ipsum</td>
            <td styleName="product-qty">2</td>
            <td styleName="product-price">$275.00</td>
          </tr>
        </tbody>
      </table>
      <ul styleName="price-summary">
        <li>
          <TermValueLine>
            <span>SUBTOTAL</span>
            $150.00
          </TermValueLine>
        </li>
        <li>
          <TermValueLine>
            <span>SHIPPING</span>
            $0.00
          </TermValueLine>
        </li>
        <li>
          <TermValueLine>
            <span>TAX</span>
            $9.00
          </TermValueLine>
        </li>
      </ul>
      <TermValueLine styleName="grand-total">
        <span>GRAND TOTAL</span>
        $159.00
      </TermValueLine>
    </div>
  );
};

export default OrderSummary;
