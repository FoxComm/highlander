import makeRouter from 'koa-router';
import { Mandrill } from 'mandrill-api/mandrill';
import util from 'util';

function *sendMessage(mandrillClient, params) {
  return new Promise((resolve, reject) => {
    mandrillClient.messages.send(params, result => {
      resolve(result);
      console.log('RESOLVED : ' + util.inspect(result));
    }, error => {
      const err = new Error(error.message || error);
      reject(err);
      console.log('REJECTED :' + error.message);
    });
  });
}

export default function mandrillRouter(apiKey) {
  const mandrillClient = new Mandrill(apiKey);

  const router = makeRouter()
    .post('/api/local/contact-feedback', function*() {
      const { name, email, phone, subject, text } = this.request.body;
      const message = {
        text: `${text}\nPhone: ${phone}`,
        subject,
        from_email: email,
        from_name: name,
        to: [{
          email: process.env.CONTACT_EMAIL,
          name: 'The Perfect Gourmet',
          type: 'to',
        }],
      };

      console.log('WE ARE SENDING!', message);
      yield sendMessage(mandrillClient, {
        message,
        async: false,
      });

      this.body = {};
    });

  return router.routes();
}
