/*
 * Copyright (c) JForum Team
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, 
 * with or without modification, are permitted provided 
 * that the following conditions are met:
 * 
 * 1) Redistributions of source code must retain the above 
 * copyright notice, this list of conditions and the 
 * following disclaimer.
 * 2) Redistributions in binary form must reproduce the 
 * above copyright notice, this list of conditions and 
 * the following disclaimer in the documentation and/or 
 * other materials provided with the distribution.
 * 3) Neither the name of "Rafael Steil" nor 
 * the names of its contributors may be used to endorse 
 * or promote products derived from this software without 
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT 
 * HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, 
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL 
 * THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE 
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER 
 * IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 * 
 * Created on 08/07/2007 11:29:41
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.dao.generic;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.jforum.JForumExecutionContext;
import net.jforum.dao.ModerationLogDAO;
import net.jforum.entities.ModerationLog;
import net.jforum.entities.User;
import net.jforum.exceptions.DatabaseException;
import net.jforum.util.DbUtils;
import net.jforum.util.preferences.SystemGlobals;

/**
 * @author Rafael Steil
 * @version $Id$
 */
public class GenericModerationLogDAO extends AutoKeys implements ModerationLogDAO
{
	@Override public void add(ModerationLog log)
	{
		PreparedStatement pstmt = null;
		
		try {
			pstmt = this.getStatementForAutoKeys("ModerationLog.addNew");
			pstmt.setInt(1, log.getUser().getId());
			pstmt.setString(2, log.getDescription());
			pstmt.setString(3, log.getOriginalMessage());
			pstmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
			pstmt.setInt(5, log.getType());
			pstmt.setInt(6, log.getPostId());
			pstmt.setInt(7, log.getTopicId());
			pstmt.setInt(8, log.getPosterUser().getId());

			this.setAutoGeneratedKeysQuery(SystemGlobals.getSql("ModerationLog.lastGeneratedModerationLogId"));
			
			int logId = this.executeAutoKeysQuery(pstmt);
			
			log.setId(logId);
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(pstmt);
		}
	}

	@Override public List<ModerationLog> selectAll(int start, int count)
	{
		List<ModerationLog> l = new ArrayList<ModerationLog>();

		String sql = SystemGlobals.getSql("ModerationLog.selectAll");

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(sql);
			pstmt.setInt(1, start);
			pstmt.setInt(2, count);

			rs = pstmt.executeQuery();

			while (rs.next()) {
				l.add(this.makeLog(rs));
			}

			return l;
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(rs, pstmt);
		}
	}
	
	protected ModerationLog makeLog(ResultSet rs) throws SQLException 
	{
		ModerationLog log = new ModerationLog();
		
		log.setId(rs.getInt("log_id"));
		log.setDescription(this.readDesriptionFromResultSet(rs));
		log.setOriginalMessage(this.readOriginalMessageFromResultSet(rs));
		log.setType(rs.getInt("log_type"));
		log.setDate(new Date(rs.getTimestamp("log_date").getTime()));
		log.setPostId(rs.getInt("post_id"));
		log.setTopicId(rs.getInt("topic_id"));
		
		User user = new User();
		user.setId(rs.getInt("user_id"));
		user.setUsername(rs.getString("username"));
		
		log.setUser(user);
		
		User posterUser = new User();
		posterUser.setId(rs.getInt("post_user_id"));
		posterUser.setUsername(rs.getString("poster_username"));
		
		log.setPosterUser(posterUser);
		
		return log;
	}
	
	protected String readDesriptionFromResultSet(ResultSet rs) throws SQLException
	{
		return rs.getString("log_description");
	}
	
	protected String readOriginalMessageFromResultSet(ResultSet rs) throws SQLException
	{
		return rs.getString("log_original_message");
	}
	
	@Override public int totalRecords()
	{
		int total = 0;
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			pstmt = JForumExecutionContext.getConnection().prepareStatement(
				SystemGlobals.getSql("ModerationLog.totalRecords"));

			rs = pstmt.executeQuery();
			
			if (rs.next()) {
				total = rs.getInt(1);
			}
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
		finally {
			DbUtils.close(rs, pstmt);
		}
		
		return total;
	}
}
