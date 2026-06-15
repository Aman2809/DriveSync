// src/contexts/AuthContext.jsx
import { createContext, useContext, useReducer, useEffect } from "react";
import { 
  // getCurrentUser,
  // logoutUser, 
  connectGoogleDrive, 
  connectDropbox, 
  connectOneDrive, 
  fetchConnectedDrives 
} from "../api/driveApi";

import { 
  getCurrentUser, 
  logoutUser,
} from "../api/oauthApi";


// ===== ACTION TYPES =====
const AUTH_ACTIONS = {
  SET_LOADING: "SET_LOADING",
  SET_USER: "SET_USER",
  CLEAR_USER: "CLEAR_USER",
  SET_ERROR: "SET_ERROR",
};

// ===== INITIAL STATE =====
const initialState = {
  loading: false,
  error: null,
  user: null,
  connectedDrives: {
    google: false,
    dropbox: false,
    onedrive: false,
  },
};

// ===== REDUCER =====
const authReducer = (state, action) => {
  switch (action.type) {
    case AUTH_ACTIONS.SET_LOADING:
      return { ...state, loading: action.payload };

    case AUTH_ACTIONS.SET_USER:
      return {
        ...state,
        user: action.payload.user,
        connectedDrives: action.payload.connectedDrives,
        loading: false,
        error: null,
      };

    case AUTH_ACTIONS.CLEAR_USER:
      return { ...initialState };

    case AUTH_ACTIONS.SET_ERROR:
      return { ...state, error: action.payload, loading: false };

    default:
      return state;
  }
};

// ===== CONTEXT =====
const AuthContext = createContext();

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used inside AuthProvider");
  }
  return ctx;
};

// ===== PROVIDER =====
export const AuthProvider = ({ children }) => {
  const [state, dispatch] = useReducer(authReducer, initialState);

  // Fetch user & connected drives on app start
  useEffect(() => {
    const fetchUserAndDrives = async () => {
      try {
        dispatch({ type: AUTH_ACTIONS.SET_LOADING, payload: true });

        const user = await getCurrentUser();
        if (!user) {
          dispatch({ type: AUTH_ACTIONS.CLEAR_USER });
          return;
        }

        // fetch connected drives from backend
        const drives = await fetchConnectedDrives(user.id);

        const connectedDrives = {
          google: drives.some((d) => d.id === "google"),
          dropbox: drives.some((d) => d.id === "dropbox"),
          onedrive: drives.some((d) => d.id === "onedrive"),
        };

        dispatch({
          type: AUTH_ACTIONS.SET_USER,
          payload: { user, connectedDrives },
        });
      } catch (err) {
        console.warn("Auth init failed:", err);
        dispatch({ type: AUTH_ACTIONS.CLEAR_USER });
      }
    };

    fetchUserAndDrives();
  }, []);

  // ✅ Helper: check if any drives connected
  const hasConnectedDrives = () => {
    return Object.values(state.connectedDrives).some((v) => v === true);
  };

  // ✅ Logout using API
  const logout = async () => {
    try {
      await logoutUser();
      dispatch({ type: AUTH_ACTIONS.CLEAR_USER });
    } catch (err) {
      console.error("Logout failed", err);
    }
  };

  // ===== CONTEXT VALUE =====
  const value = {
    ...state,
    connectGoogleDrive,
    connectDropbox,
    connectOneDrive,
    logout,
    hasConnectedDrives,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
