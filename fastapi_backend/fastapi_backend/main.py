import traceback
import os

from dotenv import load_dotenv
from urllib.parse import urlparse
from fastapi import FastAPI, HTTPException
from supabase import Client, create_client

# Load environment variables from .env
load_dotenv(override=True)

# Get Supabase credentials
SUPABASE_URL = os.getenv("SUPABASE_URL")
SUPABASE_KEY = os.getenv("SUPABASE_KEY")

parsed_url = urlparse(SUPABASE_URL or "")

print("Supabase URL host:", parsed_url.hostname)
print("Supabase URL path:", repr(parsed_url.path))

# Check if credentials are available
if not SUPABASE_URL or not SUPABASE_KEY:
    raise RuntimeError(
        "Supabase credentials are missing. Check your .env file."
    )

# Connect to Supabase
supabase: Client = create_client(
    SUPABASE_URL,
    SUPABASE_KEY
)

# Create FastAPI application
app = FastAPI(title="UniRoom API")


# Welcome endpoint
@app.get("/")
def home():
    return {
        "message": "Welcome to UniRoom API",
        "status": "running"
    }


# Health-check endpoint
@app.get("/health")
def health_check():
    return {
        "status": "healthy"
    }


# Retrieve departments from Supabase
@app.get("/departments")
def get_departments():
    try:
        response = (
            supabase
            .table("departments")
            .select("department_id, department_name")
            .limit(5)
            .execute()
        )

        return {
            "status": "success",
            "departments": response.data
        }

    except Exception:
        raise HTTPException(
            status_code=500,
            detail=(
                "Could not retrieve departments. "
                "Check your Supabase connection, table name, "
                "and Row Level Security permissions."
            )
        )
        # Retrieve rooms from Supabase
@app.get("/rooms")
def get_rooms():
    try:
        response = (
            supabase
            .table("rooms")
            .select(
                "room_id, department_id, room_number, "
                "building, floor, capacity, room_status"
            )
            .limit(20)
            .execute()
        )

        return {
            "status": "success",
            "count": len(response.data),
            "rooms": response.data
        }

    except Exception:
        raise HTTPException(
            status_code=500,
            detail="Could not retrieve rooms. Check your Supabase connection and permissions."
        )
        # Retrieve schedules from Supabase
# Retrieve schedules from Supabase
@app.get("/schedules")
def get_schedules():
    try:
        response = (
            supabase
            .table("schedules")
            .select("*")
            .limit(5)
            .execute()
        )

        schedules = response.data

        # Get readable details for each schedule
        for schedule in schedules:

            # Find room details
            room_response = (
                supabase
                .table("rooms")
                .select("room_number, building, floor")
                .eq("room_id", schedule["room_id"])
                .execute()
            )

            # Find subject details
            subject_response = (
                supabase
                .table("subjects")
                .select("subject_code, subject_name")
                .eq("subject_id", schedule["subject_id"])
                .execute()
            )

            # Find faculty name
            faculty_response = (
    supabase
    .table("faculty_directory")
    .select("first_name, last_name")
    .eq("faculty_id", schedule["faculty_id"])
    .execute()
)

            # Find section details
            section_response = (
                supabase
                .table("sections")
                .select("program, year_level, section_name")
                .eq("section_id", schedule["section_id"])
                .execute()
            )

            schedule["room"] = (
                room_response.data[0]
                if room_response.data else None
            )

            schedule["subject"] = (
                subject_response.data[0]
                if subject_response.data else None
            )

            schedule["faculty"] = (
                faculty_response.data[0]
                if faculty_response.data else None
            )

            schedule["section"] = (
                section_response.data[0]
                if section_response.data else None
            )

        return {
            "status": "success",
            "count": len(schedules),
            "schedules": schedules
        }

    except Exception:
        traceback.print_exc()

        raise HTTPException(
            status_code=500,
            detail="Could not retrieve schedules and related details."
        )