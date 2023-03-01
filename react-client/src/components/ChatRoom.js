import React, { useEffect, useState } from 'react';
import { over } from 'stompjs';
import SockJS from 'sockjs-client';

var stompClient = null;

const ChatRoom = () => {
  const [posts, setPosts] = useState([]);
  const [comments, setComments] = useState(new Map());
  const [tab, setTab] = useState();
  const [user, setUser] = useState({
    username: '',
    password: '',
    accessToken: '',
  });
  const [isLogin, setIsLogin] = useState(false);
  const [commentInput, setCommentInput] = useState('');
  const [commentPrev, setCommentPrev] = useState();

  useEffect(() => {
    const requestOptions = {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    };
    const fetchData = async () => {
      const postsData = await (await fetch(`http://localhost:8080/api/post`, requestOptions)).json();

      for (const post of postsData) {
        const commentsData = await (
          await fetch(`http://localhost:8080/api/post/${post.id}/comments`, requestOptions)
        ).json();
        comments.set(post.id, commentsData);
        setComments(new Map(comments));
      }
      setPosts(postsData);
      setTab(postsData[0].id);
    };
    fetchData();
  }, []);

  // connect SocketJS
  const connect = () => {
    let socket = new SockJS('http://localhost:8080/ws');
    stompClient = over(socket);
    stompClient.connect({}, onConnected, onError);
  };

  const onConnected = (frame) => {
    // subcribe get error, doan dùng userId chứ không phải username
    stompClient.subscribe('/user/' + user.username + '/error', onError);

    posts.forEach((post) => stompClient.subscribe('/post/' + post.id + '/comments', onSubcribe));
  };

  const onError = async (frame) => {
    // frame status: command: ERROR
    // handle token expired => /api/refresh_token
    // handle connect fails
    if (frame.command === 'MESSAGE') {
      const result = await (
        await fetch(`http://localhost:8080/api/login`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            username: user.username,
            password: user.password,
          }),
        })
      ).json();

      const accessToken = result.data.access_token;
      // localStorage.setItem('access_token', accessToken);
      setUser({ ...user, accessToken });
      const headers = {
        // get access_token from storage
        Authorization: 'Bearer ' + accessToken,
      };
      stompClient.send('/comment/send', { ...headers }, JSON.stringify(commentPrev));
    }

    // frame status: command: MESSAGE
    // handle error: data valid, sql, ...
    console.log(frame);
  };

  const onSubcribe = (payload) => {
    const payloadData = JSON.parse(payload.body);
    if (comments.get(payloadData.post.id)) {
      comments.get(payloadData.post.id).push(payloadData);
      setComments(new Map(comments));
    }
  };

  const sendValue = () => {
    if (stompClient) {
      const comment = {
        message: commentInput,
        post: {
          id: tab,
        },
      };
      const headers = {
        // get access_token from storage
        Authorization: 'Bearer ' + user.accessToken,
      };
      stompClient.send('/comment/send', { ...headers }, JSON.stringify(comment));
      setCommentPrev(comment);
      setCommentInput('');
    }
  };

  const handleLogin = async (event) => {
    event.preventDefault();
    const result = await (
      await fetch(`http://localhost:8080/api/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          username: user.username,
          password: user.password,
        }),
      })
    ).json();

    const accessToken = result.data.access_token;
    // localStorage.setItem('access_token', accessToken);
    setUser({ ...user, accessToken });
    connect();
    setIsLogin(true);
  };
  return (
    <div className="container">
      {isLogin ? (
        <div className="chat-box">
          <div className="member-list">
            <ul>
              {[...posts].map((post, index) => (
                <li
                  onClick={() => {
                    setTab(post.id);
                  }}
                  className={`member ${tab === post.id && 'active'}`}
                  key={index}
                >
                  {post.title}
                </li>
              ))}
            </ul>
          </div>
          <div className="chat-content">
            <ul className="chat-messages">
              {[...comments.get(tab)].map((comment, index) => (
                <li className={`message ${comment.createdBy.username === user.username && 'self'}`} key={index}>
                  {comment.createdBy.username !== user.username && (
                    <div className="avatar">{comment.createdBy.username}</div>
                  )}
                  <div className="message-data">{comment.message}</div>
                  {comment.createdBy.username === user.username && (
                    <div className="avatar self">{comment.createdBy.username}</div>
                  )}
                </li>
              ))}
            </ul>

            <div className="send-message">
              <input
                type="text"
                className="input-message"
                placeholder="enter the message"
                value={commentInput}
                onChange={(event) => setCommentInput(event.target.value)}
              />
              <button type="button" className="send-button" onClick={sendValue}>
                send
              </button>
            </div>
          </div>
        </div>
      ) : (
        <form className="register" onSubmit={handleLogin}>
          <input
            id="user-name"
            placeholder="Enter your name"
            name="userName"
            value={user.username}
            onChange={(event) => setUser({ ...user, username: event.target.value })}
            margin="normal"
          />
          <input
            id="user-name"
            placeholder="Enter your name"
            name="userName"
            value={user.password}
            onChange={(event) => setUser({ ...user, password: event.target.value })}
            margin="normal"
          />
          <button type="submit">connect</button>
        </form>
      )}
    </div>
  );
};

export default ChatRoom;
