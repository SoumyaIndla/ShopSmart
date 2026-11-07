
import { useState } from "react";
import api from "./services/api";
import "./Auth.css";

function Auth({ onAuth }) {
    const [isLogin, setIsLogin] = useState(true);
    const [fullName, setFullName] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    async function handleSubmit(event) {
        event.preventDefault();
        setError("");
        setLoading(true);

        try {
            const endpoint = isLogin
                ? "/api/auth/login"
                : "/api/auth/register";

            const requestData = isLogin
                ? { email, password }
                : { fullName, email, password };

            const response = await api.post(endpoint, requestData);
            const authData = response.data;

            if (!authData.token) {
                throw new Error("Authentication token was not returned.");
            }

            sessionStorage.setItem("shopsmart_token", authData.token);
            sessionStorage.setItem("shopsmart_user", JSON.stringify({
                email: authData.email,
                fullName: authData.fullName,
                role: authData.role,
            }));

            onAuth(authData);
        } catch (err) {
            const message =
                err.response?.data?.message ||
                err.message ||
                "Authentication failed. Please try again.";

            setError(message);
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="auth-page">
            <div className="auth-card">
                <h1>ShopSmart</h1>
                <h2>{isLogin ? "Welcome Back!" : "Create Your Account"}</h2>

                <p className="auth-subtitle">
                    {isLogin
                        ? "Login to continue shopping."
                        : "Register to start shopping with ShopSmart."}
                </p>

                <form onSubmit={handleSubmit}>
                    {!isLogin && (
                        <label>
                            Full Name
                            <input
                                type="text"
                                value={fullName}
                                onChange={(event) => setFullName(event.target.value)}
                                placeholder="Enter your full name"
                                maxLength={80}
                                required
                            />
                        </label>
                    )}

                    <label>
                        Email
                        <input
                            type="email"
                            value={email}
                            onChange={(event) => setEmail(event.target.value)}
                            placeholder="Enter your email"
                            maxLength={160}
                            required
                        />
                    </label>

                    <label>
                        Password
                        <input
                            type="password"
                            value={password}
                            onChange={(event) => setPassword(event.target.value)}
                            placeholder="Enter your password"
                            minLength={isLogin ? 1 : 8}
                            maxLength={72}
                            required
                        />
                    </label>

                    {error && <p className="auth-error">{error}</p>}

                    <button type="submit" disabled={loading}>
                        {loading
                            ? "Please wait..."
                            : isLogin
                                ? "Login"
                                : "Create Account"}
                    </button>
                </form>

                <p className="auth-switch">
                    {isLogin
                        ? "Don't have an account?"
                        : "Already have an account?"}

                    <button
                        type="button"
                        onClick={() => {
                            setIsLogin(!isLogin);
                            setError("");
                        }}
                    >
                        {isLogin ? "Register" : "Login"}
                    </button>
                </p>
            </div>
        </div>
    );
}

export default Auth;
