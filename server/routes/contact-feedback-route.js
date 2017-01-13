import makeRouter from 'koa-router';
import { Mandrill } from 'mandrill-api/mandrill';

function formatMessage(data) {
  const {
    name = '',
    email = '',
    phone = '',
    subject = '',
    text = '',
  } = data;

  return (
    `Name: ${name}\n` +
    `Email: ${email}\n` +
    `Phone: ${phone}\n` +
    `Subject: ${subject}\n` +
    `Message:\n${text}`
  );
}

function *sendMessage(mandrillClient, params) {
  return new Promise((resolve, reject) => {
    mandrillClient.messages.send(params, ([result]) => {
      if (result.status === 'rejected') {
        reject(new Error(`Rejected. Reason: ${result.reject_reason}`));
      } else {
        resolve(result);
      }
    }, error => {
      const err = new Error(error.message || error);
      reject(err);
    });
  });
}

export default function mandrillRouter(apiKey) {
  const mandrillClient = new Mandrill(apiKey);

  const router = makeRouter()
    .post('/api/local/contact-feedback', function*() {
      const { name, email, subject } = this.request.body;
      const text = formatMessage(this.request.body);
      const message = {
        text,
        subject,
        from_email: process.env.CONTACT_EMAIL, // send message from verified mandrill email
        from_name: name,
        to: [{
          email: process.env.CONTACT_EMAIL,
          type: 'to',
        }],
        headers: {
          'Reply-To': email,
        },
      };

      try {
        yield sendMessage(mandrillClient, { message });
        this.body = {};
      } catch (err) {
        this.status = 500;
        this.body = { errors: [err.message] };
        this.app.emit('error', err, this);
      }
    });

  return router.routes();
}
